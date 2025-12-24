from fastapi import FastAPI, Depends, HTTPException, Request, Form, Query
from fastapi.responses import HTMLResponse, RedirectResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from sqlalchemy import func, and_
from datetime import datetime, date, timedelta
from contextlib import asynccontextmanager
from typing import Optional, List
import hashlib
import json
import os

# 현재 파일 기준 경로 설정
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

from database import (
    init_db, get_db,
    User, RegisteredPC, NaverAccount, BlogDistribution, TaskAssignment, Setting, TaskHistory
)
from crawler import PlaceCrawler

def hash_password(password: str) -> str:
    return hashlib.sha256(password.encode()).hexdigest()

@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    db = next(get_db())
    admin = db.query(User).filter(User.username == "admin").first()
    if not admin:
        admin = User(username="admin", hashed_password=hash_password("admin123"), is_admin=True)
        db.add(admin)
        db.commit()

    default_settings = [
        ("chatgpt_api_key", ""),
        ("pixabay_api_key", ""),
        ("default_prompt", ""),
    ]
    for key, value in default_settings:
        if not db.query(Setting).filter(Setting.key == key).first():
            db.add(Setting(key=key, value=value))
    db.commit()
    db.close()
    yield

app = FastAPI(title="블로그 자동화 관리", lifespan=lifespan)

# 정적 파일과 템플릿 경로 설정
static_dir = os.path.join(BASE_DIR, "static")
templates_dir = os.path.join(BASE_DIR, "templates")

if os.path.exists(static_dir):
    app.mount("/static", StaticFiles(directory=static_dir), name="static")
templates = Jinja2Templates(directory=templates_dir)

# ============================================
# 대시보드
# ============================================
@app.get("/", response_class=HTMLResponse)
async def dashboard(request: Request, db: Session = Depends(get_db)):
    today = date.today()
    stats = {
        "total_accounts": db.query(NaverAccount).filter(NaverAccount.is_active == True).count(),
        "total_distributions": db.query(BlogDistribution).filter(BlogDistribution.is_active == True).count(),
        "pending_tasks": db.query(TaskAssignment).filter(TaskAssignment.status == "pending").count(),
        "today_tasks": db.query(TaskAssignment).filter(TaskAssignment.scheduled_date == today).count(),
        "completed_tasks": db.query(TaskAssignment).filter(TaskAssignment.status == "completed").count(),
        "failed_tasks": db.query(TaskAssignment).filter(TaskAssignment.status == "fail").count(),
        "active_pcs": db.query(RegisteredPC).filter(RegisteredPC.is_active == True).count(),
    }
    recent_tasks = db.query(TaskAssignment).order_by(TaskAssignment.created_at.desc()).limit(10).all()
    return templates.TemplateResponse("dashboard.html", {
        "request": request,
        "stats": stats,
        "recent_tasks": recent_tasks
    })

# ============================================
# 네이버 계정 관리
# ============================================
@app.get("/accounts", response_class=HTMLResponse)
async def accounts_page(request: Request, db: Session = Depends(get_db)):
    accounts = db.query(NaverAccount).order_by(NaverAccount.created_at.desc()).all()
    return templates.TemplateResponse("accounts.html", {"request": request, "accounts": accounts})

@app.post("/accounts/add")
async def add_account(
    naver_id: str = Form(...),
    naver_pw: str = Form(...),
    proxy_ip: str = Form(None),
    daily_limit: int = Form(10),
    db: Session = Depends(get_db)
):
    if db.query(NaverAccount).filter(NaverAccount.naver_id == naver_id).first():
        raise HTTPException(status_code=400, detail="이미 등록된 계정입니다")
    account = NaverAccount(
        naver_id=naver_id, naver_pw=naver_pw,
        proxy_ip=proxy_ip if proxy_ip else None,
        daily_limit=daily_limit
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

# ============================================
# 블로그 배포 관리
# ============================================
@app.get("/distributions", response_class=HTMLResponse)
async def distributions_page(request: Request, db: Session = Depends(get_db)):
    distributions = db.query(BlogDistribution).filter(BlogDistribution.is_active == True).order_by(BlogDistribution.created_at.desc()).all()
    return templates.TemplateResponse("distributions.html", {"request": request, "distributions": distributions})

@app.get("/distributions/new", response_class=HTMLResponse)
async def new_distribution_page(request: Request, db: Session = Depends(get_db)):
    accounts = db.query(NaverAccount).filter(NaverAccount.is_active == True).all()
    return templates.TemplateResponse("distribution_form.html", {"request": request, "accounts": accounts})

@app.post("/api/place/lookup")
async def lookup_place(url: str = Form(...)):
    """플레이스 URL 조회"""
    # URL 유효성 검사
    validation = PlaceCrawler.validate_url(url)
    if not validation["valid"]:
        return JSONResponse({"success": False, "error": validation["error"]})

    # 크롤링
    result = await PlaceCrawler.fetch_place_info(url)
    return JSONResponse(result)

@app.post("/distributions/add")
async def add_distribution(
    place_url: str = Form(...),
    place_name: str = Form(...),
    place_address: str = Form(...),
    place_category: str = Form(None),
    distribution_category: str = Form(...),
    image_option: str = Form("none"),
    image_urls: str = Form(None),
    google_drive_url: str = Form(None),
    pixabay_keyword: str = Form(None),
    start_date: str = Form(...),
    daily_task_count: int = Form(1),
    total_days: int = Form(1),
    main_keyword: str = Form(...),
    forbidden_words: str = Form(None),
    bottom_tags: str = Form(None),
    prompt_template: str = Form(None),
    db: Session = Depends(get_db)
):
    # 시작일 파싱
    start = datetime.strptime(start_date, "%Y-%m-%d").date()
    end = start + timedelta(days=total_days - 1)

    # 배포 생성
    distribution = BlogDistribution(
        place_url=place_url,
        place_name=place_name,
        place_address=place_address,
        place_category=place_category,
        distribution_category=distribution_category,
        image_option=image_option,
        image_urls=image_urls,
        google_drive_url=google_drive_url,
        pixabay_keyword=pixabay_keyword,
        start_date=start,
        daily_task_count=daily_task_count,
        total_days=total_days,
        expected_end_date=end,
        main_keyword=main_keyword,
        forbidden_words=forbidden_words,
        bottom_tags=bottom_tags,
        prompt_template=prompt_template,
        total_tasks=daily_task_count * total_days
    )
    db.add(distribution)
    db.commit()

    # 개별 작업 생성
    for day_offset in range(total_days):
        scheduled = start + timedelta(days=day_offset)
        for _ in range(daily_task_count):
            assignment = TaskAssignment(
                distribution_id=distribution.id,
                scheduled_date=scheduled,
                status="pending"
            )
            db.add(assignment)
    db.commit()

    return RedirectResponse(url="/distributions", status_code=303)

@app.post("/distributions/delete/{dist_id}")
async def delete_distribution(dist_id: int, db: Session = Depends(get_db)):
    dist = db.query(BlogDistribution).filter(BlogDistribution.id == dist_id).first()
    if dist:
        dist.is_active = False
        dist.status = "cancelled"
        db.commit()
    return RedirectResponse(url="/distributions", status_code=303)

# ============================================
# 작업 관리
# ============================================
@app.get("/tasks", response_class=HTMLResponse)
async def tasks_page(request: Request, db: Session = Depends(get_db)):
    tasks = db.query(TaskAssignment).order_by(TaskAssignment.scheduled_date.desc(), TaskAssignment.created_at.desc()).limit(100).all()
    return templates.TemplateResponse("tasks.html", {"request": request, "tasks": tasks})

# ============================================
# PC 관리
# ============================================
@app.get("/pcs", response_class=HTMLResponse)
async def pcs_page(request: Request, db: Session = Depends(get_db)):
    pcs = db.query(RegisteredPC).order_by(RegisteredPC.created_at.desc()).all()
    return templates.TemplateResponse("pcs.html", {"request": request, "pcs": pcs})

# ============================================
# 설정
# ============================================
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
    for key, value in {"chatgpt_api_key": chatgpt_api_key, "pixabay_api_key": pixabay_api_key, "default_prompt": default_prompt}.items():
        setting = db.query(Setting).filter(Setting.key == key).first()
        if setting:
            setting.value = value
        else:
            db.add(Setting(key=key, value=value))
    db.commit()
    return RedirectResponse(url="/settings", status_code=303)

# ============================================
# JAR 클라이언트 API
# ============================================
@app.get("/api/blog-task")
async def get_blog_task(pc_hw_code: str = Query(...), db: Session = Depends(get_db)):
    """JAR 클라이언트가 다음 작업을 요청"""
    # PC 등록/업데이트
    pc = db.query(RegisteredPC).filter(RegisteredPC.pc_hw_code == pc_hw_code).first()
    if not pc:
        pc = RegisteredPC(pc_hw_code=pc_hw_code, pc_name=f"PC-{pc_hw_code[:8]}")
        db.add(pc)
    pc.last_seen = datetime.utcnow()
    pc.is_active = True
    db.commit()

    # 오늘 날짜의 대기중인 작업 찾기
    today = date.today()
    task = db.query(TaskAssignment).filter(
        TaskAssignment.status == "pending",
        TaskAssignment.scheduled_date <= today
    ).order_by(TaskAssignment.scheduled_date.asc()).first()

    if not task:
        return {"success": False, "message": "할당된 작업이 없습니다"}

    distribution = task.distribution
    if not distribution:
        return {"success": False, "message": "배포 정보를 찾을 수 없습니다"}

    # 사용 가능한 계정 찾기
    if not task.naver_account_id:
        account = db.query(NaverAccount).filter(
            NaverAccount.is_active == True,
            NaverAccount.status == "active"
        ).first()
        if account:
            task.naver_account_id = account.id
            db.commit()

    account = task.naver_account
    if not account:
        return {"success": False, "message": "사용 가능한 계정이 없습니다"}

    # 이미지 URL 파싱
    image_urls = []
    if distribution.image_urls:
        try:
            image_urls = json.loads(distribution.image_urls)
        except:
            image_urls = [u.strip() for u in distribution.image_urls.split(",") if u.strip()]

    return {
        "success": True,
        "taskId": task.id,
        "assignmentId": task.id,
        "placeName": distribution.place_name,
        "placeAddress": distribution.place_address,
        "placeOriginAddress": distribution.place_address,
        "placeUrl": distribution.place_url or "",
        "mainKeyword": distribution.main_keyword,
        "bottomTagsString": distribution.bottom_tags or "",
        "forbiddenWordsString": distribution.forbidden_words or "",
        "taskCount": 1,
        "prompt": distribution.prompt_template or "",
        "category": distribution.distribution_category or "",
        "imageOption": distribution.image_option,
        "imageUrls": image_urls,
        "googleDriveUrl": distribution.google_drive_url or "",
        "pixabayKeyword": distribution.pixabay_keyword or "",
        "naverAccount": {
            "id": account.naver_id,
            "pw": account.naver_pw,
            "proxyIp": account.proxy_ip or ""
        }
    }

@app.post("/api/blog-task/start")
async def start_blog_task(task_id: int = Form(...), pc_hw_code: str = Form(...), db: Session = Depends(get_db)):
    task = db.query(TaskAssignment).filter(TaskAssignment.id == task_id).first()
    if task:
        task.status = "in_progress"
        task.pc_hw_code = pc_hw_code
        task.started_at = datetime.utcnow()
        db.add(TaskHistory(assignment_id=task_id, action="started", details=f"PC: {pc_hw_code}"))
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
    task = db.query(TaskAssignment).filter(TaskAssignment.id == task_id).first()
    if task:
        task.status = status
        task.result_post_url = result_url
        task.post_title = post_title
        task.completed_at = datetime.utcnow()

        # 배포 통계 업데이트
        dist = task.distribution
        if dist:
            if status == "completed":
                dist.completed_tasks += 1
            else:
                dist.failed_tasks += 1
            if dist.completed_tasks + dist.failed_tasks >= dist.total_tasks:
                dist.status = "completed"

        db.add(TaskHistory(assignment_id=task_id, action=status, details=f"URL: {result_url}"))
        db.commit()
    return {"success": True}

@app.post("/api/account/status")
async def update_account_status(naver_id: str = Form(...), status: str = Form(...), db: Session = Depends(get_db)):
    account = db.query(NaverAccount).filter(NaverAccount.naver_id == naver_id).first()
    if account:
        account.status = status
        db.commit()
    return {"success": True}

@app.get("/api/deleted-tasks")
async def get_deleted_tasks(pc_hw_code: str = Query(...), db: Session = Depends(get_db)):
    tasks = db.query(TaskAssignment).filter(TaskAssignment.status == "deleted").all()
    result = []
    for task in tasks:
        if task.naver_account and task.distribution:
            result.append({
                "taskId": task.id,
                "id": task.naver_account.naver_id,
                "pw": task.naver_account.naver_pw,
                "blogUrl": task.result_post_url,
                "placeName": task.distribution.place_name,
                "proxyIp": task.naver_account.proxy_ip or ""
            })
    return result

@app.post("/api/deletion-complete")
async def report_deletion_complete(task_ids: str = Form(...), db: Session = Depends(get_db)):
    try:
        for task_id in json.loads(task_ids):
            task = db.query(TaskAssignment).filter(TaskAssignment.id == task_id).first()
            if task:
                task.status = "deleted_confirmed"
        db.commit()
    except:
        pass
    return {"success": True}

@app.get("/api/chatgpt-key")
async def get_chatgpt_key(db: Session = Depends(get_db)):
    setting = db.query(Setting).filter(Setting.key == "chatgpt_api_key").first()
    return {"apiKey": setting.value if setting else ""}

@app.get("/api/prompt/{category}")
async def get_prompt_by_category(category: str, db: Session = Depends(get_db)):
    setting = db.query(Setting).filter(Setting.key == f"prompt_{category}").first()
    if not setting:
        setting = db.query(Setting).filter(Setting.key == "default_prompt").first()
    return {"prompt": setting.value if setting else ""}

# ============================================
# Python 클라이언트 API
# ============================================
@app.get("/api/health")
async def health_check():
    """서버 상태 확인"""
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}

@app.post("/api/pc/register")
async def register_pc_api(request: Request, db: Session = Depends(get_db)):
    """PC 등록 API"""
    try:
        data = await request.json()
        hw_code = data.get("hw_code")
        pc_name = data.get("pc_name", f"PC-{hw_code[:8]}")

        pc = db.query(RegisteredPC).filter(RegisteredPC.pc_hw_code == hw_code).first()
        if not pc:
            pc = RegisteredPC(pc_hw_code=hw_code, pc_name=pc_name)
            db.add(pc)

        pc.pc_name = pc_name
        pc.last_seen = datetime.utcnow()
        pc.is_active = True
        db.commit()

        return {"success": True, "pc_id": pc.id}
    except Exception as e:
        return {"success": False, "error": str(e)}

@app.get("/api/settings")
async def get_settings_api(db: Session = Depends(get_db)):
    """설정 가져오기 API (JSON)"""
    settings = {s.key: s.value for s in db.query(Setting).all()}
    return settings

@app.get("/api/task/get")
async def get_task_api(hw_code: str = Query(...), db: Session = Depends(get_db)):
    """작업 가져오기 API"""
    # PC 업데이트
    pc = db.query(RegisteredPC).filter(RegisteredPC.pc_hw_code == hw_code).first()
    if pc:
        pc.last_seen = datetime.utcnow()
        db.commit()

    # 오늘 날짜의 대기중인 작업 찾기
    today = date.today()
    task = db.query(TaskAssignment).filter(
        TaskAssignment.status == "pending",
        TaskAssignment.scheduled_date <= today
    ).order_by(TaskAssignment.scheduled_date.asc()).first()

    if not task:
        return {"task": None}

    distribution = task.distribution
    if not distribution:
        return {"task": None}

    # 사용 가능한 계정 찾기
    if not task.naver_account_id:
        account = db.query(NaverAccount).filter(
            NaverAccount.is_active == True,
            NaverAccount.status == "active"
        ).first()
        if account:
            task.naver_account_id = account.id
            db.commit()

    account = task.naver_account
    if not account:
        return {"task": None, "error": "사용 가능한 계정이 없습니다"}

    return {
        "task": {
            "id": task.id,
            "distribution": {
                "place_name": distribution.place_name,
                "place_address": distribution.place_address,
                "place_url": distribution.place_url,
                "distribution_category": distribution.distribution_category,
                "main_keyword": distribution.main_keyword,
                "forbidden_words": distribution.forbidden_words,
                "bottom_tags": distribution.bottom_tags,
                "prompt_template": distribution.prompt_template,
                "image_option": distribution.image_option,
                "image_urls": distribution.image_urls,
            },
            "account": {
                "naver_id": account.naver_id,
                "naver_pw": account.naver_pw,
                "proxy_ip": account.proxy_ip
            }
        }
    }

@app.post("/api/task/update")
async def update_task_api(request: Request, db: Session = Depends(get_db)):
    """작업 상태 업데이트 API"""
    try:
        data = await request.json()
        task_id = data.get("task_id")
        status = data.get("status")
        result_url = data.get("result_url")
        error_message = data.get("error_message")
        hw_code = data.get("hw_code")

        task = db.query(TaskAssignment).filter(TaskAssignment.id == task_id).first()
        if not task:
            return {"success": False, "error": "작업을 찾을 수 없습니다"}

        task.status = status
        task.pc_hw_code = hw_code

        if status == "in_progress":
            task.started_at = datetime.utcnow()
        elif status in ["completed", "fail"]:
            task.completed_at = datetime.utcnow()
            if result_url:
                task.result_post_url = result_url
            if error_message:
                task.error_message = error_message

            # 배포 통계 업데이트
            dist = task.distribution
            if dist:
                if status == "completed":
                    dist.completed_tasks += 1
                else:
                    dist.failed_tasks += 1
                if dist.completed_tasks + dist.failed_tasks >= dist.total_tasks:
                    dist.status = "completed"

        db.add(TaskHistory(assignment_id=task_id, action=status, details=f"URL: {result_url}" if result_url else error_message))
        db.commit()

        return {"success": True}
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
