from fastapi import FastAPI, Depends, HTTPException, Request, Form, Query
from fastapi.responses import HTMLResponse, RedirectResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime
from typing import Optional, List
import json
import os

from database import (
    init_db, get_db,
    User, RegisteredPC, NaverAccount, Place, BlogTask, Setting, TaskHistory
)

app = FastAPI(title="SketchBlog Auto - Admin Panel")

# Static files and templates
app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

# Initialize database on startup
@app.on_event("startup")
async def startup():
    init_db()
    # Create default admin user if not exists
    db = next(get_db())
    admin = db.query(User).filter(User.username == "admin").first()
    if not admin:
        from passlib.hash import bcrypt
        admin = User(
            username="admin",
            hashed_password=bcrypt.hash("admin123"),
            is_admin=True
        )
        db.add(admin)
        db.commit()

    # Create default settings
    default_settings = [
        ("chatgpt_api_key", ""),
        ("pixabay_api_key", ""),
        ("default_prompt", ""),
    ]
    for key, value in default_settings:
        existing = db.query(Setting).filter(Setting.key == key).first()
        if not existing:
            db.add(Setting(key=key, value=value))
    db.commit()
    db.close()

# ============================================
# Admin Web Pages
# ============================================

@app.get("/", response_class=HTMLResponse)
async def dashboard(request: Request, db: Session = Depends(get_db)):
    stats = {
        "total_accounts": db.query(NaverAccount).filter(NaverAccount.is_active == True).count(),
        "total_places": db.query(Place).filter(Place.is_active == True).count(),
        "pending_tasks": db.query(BlogTask).filter(BlogTask.status == "pending").count(),
        "completed_tasks": db.query(BlogTask).filter(BlogTask.status == "completed").count(),
        "failed_tasks": db.query(BlogTask).filter(BlogTask.status == "fail").count(),
        "active_pcs": db.query(RegisteredPC).filter(RegisteredPC.is_active == True).count(),
    }
    recent_tasks = db.query(BlogTask).order_by(BlogTask.created_at.desc()).limit(10).all()
    return templates.TemplateResponse("dashboard.html", {
        "request": request,
        "stats": stats,
        "recent_tasks": recent_tasks
    })

@app.get("/accounts", response_class=HTMLResponse)
async def accounts_page(request: Request, db: Session = Depends(get_db)):
    accounts = db.query(NaverAccount).order_by(NaverAccount.created_at.desc()).all()
    return templates.TemplateResponse("accounts.html", {"request": request, "accounts": accounts})

@app.post("/accounts/add")
async def add_account(
    naver_id: str = Form(...),
    naver_pw: str = Form(...),
    proxy_ip: str = Form(None),
    db: Session = Depends(get_db)
):
    existing = db.query(NaverAccount).filter(NaverAccount.naver_id == naver_id).first()
    if existing:
        raise HTTPException(status_code=400, detail="Account already exists")

    account = NaverAccount(
        naver_id=naver_id,
        naver_pw=naver_pw,
        proxy_ip=proxy_ip if proxy_ip else None
    )
    db.add(account)
    db.commit()
    return RedirectResponse(url="/accounts", status_code=303)

@app.post("/accounts/delete/{account_id}")
async def delete_account(account_id: int, db: Session = Depends(get_db)):
    account = db.query(NaverAccount).filter(NaverAccount.id == account_id).first()
    if account:
        account.is_active = False
        db.commit()
    return RedirectResponse(url="/accounts", status_code=303)

@app.get("/places", response_class=HTMLResponse)
async def places_page(request: Request, db: Session = Depends(get_db)):
    places = db.query(Place).filter(Place.is_active == True).order_by(Place.created_at.desc()).all()
    return templates.TemplateResponse("places.html", {"request": request, "places": places})

@app.post("/places/add")
async def add_place(
    name: str = Form(...),
    address: str = Form(...),
    main_keyword: str = Form(...),
    category: str = Form(None),
    bottom_tags: str = Form(None),
    forbidden_words: str = Form(None),
    prompt: str = Form(None),
    image_option: str = Form("none"),
    image_urls: str = Form(None),
    google_drive_url: str = Form(None),
    pixabay_keyword: str = Form(None),
    db: Session = Depends(get_db)
):
    place = Place(
        name=name,
        address=address,
        main_keyword=main_keyword,
        category=category,
        bottom_tags=bottom_tags,
        forbidden_words=forbidden_words,
        prompt=prompt,
        image_option=image_option,
        image_urls=image_urls,
        google_drive_url=google_drive_url,
        pixabay_keyword=pixabay_keyword
    )
    db.add(place)
    db.commit()
    return RedirectResponse(url="/places", status_code=303)

@app.post("/places/delete/{place_id}")
async def delete_place(place_id: int, db: Session = Depends(get_db)):
    place = db.query(Place).filter(Place.id == place_id).first()
    if place:
        place.is_active = False
        db.commit()
    return RedirectResponse(url="/places", status_code=303)

@app.get("/tasks", response_class=HTMLResponse)
async def tasks_page(request: Request, db: Session = Depends(get_db)):
    tasks = db.query(BlogTask).order_by(BlogTask.created_at.desc()).all()
    places = db.query(Place).filter(Place.is_active == True).all()
    accounts = db.query(NaverAccount).filter(NaverAccount.is_active == True).all()
    return templates.TemplateResponse("tasks.html", {
        "request": request,
        "tasks": tasks,
        "places": places,
        "accounts": accounts
    })

@app.post("/tasks/add")
async def add_task(
    place_id: int = Form(...),
    naver_account_id: int = Form(None),
    task_count: int = Form(1),
    db: Session = Depends(get_db)
):
    task = BlogTask(
        place_id=place_id,
        naver_account_id=naver_account_id if naver_account_id else None,
        task_count=task_count,
        status="pending"
    )
    db.add(task)
    db.commit()
    return RedirectResponse(url="/tasks", status_code=303)

@app.get("/pcs", response_class=HTMLResponse)
async def pcs_page(request: Request, db: Session = Depends(get_db)):
    pcs = db.query(RegisteredPC).order_by(RegisteredPC.created_at.desc()).all()
    return templates.TemplateResponse("pcs.html", {"request": request, "pcs": pcs})

@app.get("/settings", response_class=HTMLResponse)
async def settings_page(request: Request, db: Session = Depends(get_db)):
    settings = {s.key: s.value for s in db.query(Setting).all()}
    return templates.TemplateResponse("settings.html", {"request": request, "settings": settings})

@app.post("/settings/save")
async def save_settings(
    chatgpt_api_key: str = Form(""),
    pixabay_api_key: str = Form(""),
    default_prompt: str = Form(""),
    db: Session = Depends(get_db)
):
    settings_data = {
        "chatgpt_api_key": chatgpt_api_key,
        "pixabay_api_key": pixabay_api_key,
        "default_prompt": default_prompt
    }
    for key, value in settings_data.items():
        setting = db.query(Setting).filter(Setting.key == key).first()
        if setting:
            setting.value = value
        else:
            db.add(Setting(key=key, value=value))
    db.commit()
    return RedirectResponse(url="/settings", status_code=303)

# ============================================
# API Endpoints (for JAR client)
# ============================================

@app.get("/api/blog-task")
async def get_blog_task(
    pc_hw_code: str = Query(...),
    db: Session = Depends(get_db)
):
    """Get next pending task for the given PC"""
    # Register/update PC
    pc = db.query(RegisteredPC).filter(RegisteredPC.pc_hw_code == pc_hw_code).first()
    if not pc:
        pc = RegisteredPC(pc_hw_code=pc_hw_code, pc_name=f"PC-{pc_hw_code[:8]}")
        db.add(pc)
    pc.last_seen = datetime.utcnow()
    pc.is_active = True
    db.commit()

    # Find pending task
    task = db.query(BlogTask).filter(
        BlogTask.status == "pending"
    ).order_by(BlogTask.created_at.asc()).first()

    if not task:
        return {"success": False, "message": "No pending tasks"}

    place = task.place

    # Get available account if not assigned
    if not task.naver_account_id:
        available_account = db.query(NaverAccount).filter(
            NaverAccount.is_active == True,
            NaverAccount.status == "active"
        ).first()
        if available_account:
            task.naver_account_id = available_account.id
            db.commit()

    account = task.naver_account
    if not account:
        return {"success": False, "message": "No available accounts"}

    # Parse image URLs
    image_urls = []
    if place.image_urls:
        try:
            image_urls = json.loads(place.image_urls)
        except:
            image_urls = [url.strip() for url in place.image_urls.split(",") if url.strip()]

    return {
        "success": True,
        "taskId": task.id,
        "assignmentId": task.id,
        "placeName": place.name,
        "placeAddress": place.address,
        "placeOriginAddress": place.origin_address or place.address,
        "placeUrl": place.url or "",
        "mainKeyword": place.main_keyword,
        "bottomTagsString": place.bottom_tags or "",
        "forbiddenWordsString": place.forbidden_words or "",
        "taskCount": task.task_count,
        "prompt": place.prompt or "",
        "category": place.category or "",
        "imageOption": place.image_option,
        "imageUrls": image_urls,
        "googleDriveUrl": place.google_drive_url or "",
        "pixabayKeyword": place.pixabay_keyword or "",
        "naverAccount": {
            "id": account.naver_id,
            "pw": account.naver_pw,
            "proxyIp": account.proxy_ip or ""
        }
    }

@app.post("/api/blog-task/start")
async def start_blog_task(
    task_id: int = Form(...),
    pc_hw_code: str = Form(...),
    db: Session = Depends(get_db)
):
    """Mark task as started"""
    task = db.query(BlogTask).filter(BlogTask.id == task_id).first()
    if task:
        task.status = "in_progress"
        task.pc_hw_code = pc_hw_code
        task.assigned_at = datetime.utcnow()
        db.add(TaskHistory(task_id=task_id, action="started", details=f"PC: {pc_hw_code}"))
        db.commit()
    return {"success": True}

@app.post("/api/blog-task/complete")
async def complete_blog_task(
    task_id: int = Form(...),
    pc_hw_code: str = Form(...),
    naver_id: str = Form(...),
    result_url: str = Form(None),
    status: str = Form(...),
    assignment_id: int = Form(...),
    post_title: str = Form(None),
    db: Session = Depends(get_db)
):
    """Report task completion"""
    task = db.query(BlogTask).filter(BlogTask.id == task_id).first()
    if task:
        task.status = status
        task.result_post_url = result_url
        task.post_title = post_title
        task.completed_at = datetime.utcnow()
        db.add(TaskHistory(
            task_id=task_id,
            action="completed" if status == "completed" else "failed",
            details=f"URL: {result_url}"
        ))
        db.commit()
    return {"success": True}

@app.post("/api/account/status")
async def update_account_status(
    naver_id: str = Form(...),
    status: str = Form(...),
    db: Session = Depends(get_db)
):
    """Update Naver account status"""
    account = db.query(NaverAccount).filter(NaverAccount.naver_id == naver_id).first()
    if account:
        account.status = status
        db.commit()
    return {"success": True}

@app.get("/api/deleted-tasks")
async def get_deleted_tasks(
    pc_hw_code: str = Query(...),
    db: Session = Depends(get_db)
):
    """Get tasks marked for deletion"""
    tasks = db.query(BlogTask).filter(BlogTask.status == "deleted").all()
    result = []
    for task in tasks:
        if task.naver_account:
            result.append({
                "taskId": task.id,
                "id": task.naver_account.naver_id,
                "pw": task.naver_account.naver_pw,
                "blogUrl": task.result_post_url,
                "placeName": task.place.name if task.place else "",
                "proxyIp": task.naver_account.proxy_ip or ""
            })
    return result

@app.post("/api/deletion-complete")
async def report_deletion_complete(
    task_ids: str = Form(...),
    db: Session = Depends(get_db)
):
    """Report deletion completed"""
    try:
        ids = json.loads(task_ids)
        for task_id in ids:
            task = db.query(BlogTask).filter(BlogTask.id == task_id).first()
            if task:
                task.status = "deleted_confirmed"
                db.commit()
    except:
        pass
    return {"success": True}

@app.get("/api/chatgpt-key")
async def get_chatgpt_key(db: Session = Depends(get_db)):
    """Get ChatGPT API key"""
    setting = db.query(Setting).filter(Setting.key == "chatgpt_api_key").first()
    return {"apiKey": setting.value if setting else ""}

@app.get("/api/prompt/{category}")
async def get_prompt_by_category(category: str, db: Session = Depends(get_db)):
    """Get prompt by category"""
    setting = db.query(Setting).filter(Setting.key == f"prompt_{category}").first()
    if not setting:
        setting = db.query(Setting).filter(Setting.key == "default_prompt").first()
    return {"prompt": setting.value if setting else ""}

# ============================================
# Run server
# ============================================
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
