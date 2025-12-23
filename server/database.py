from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime, Boolean, ForeignKey, Date, Float
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
from datetime import datetime, date
import enum

SQLALCHEMY_DATABASE_URL = "sqlite:///./blog_auto.db"

engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Admin User
class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True)
    hashed_password = Column(String(255))
    is_admin = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

# Registered PCs
class RegisteredPC(Base):
    __tablename__ = "registered_pcs"

    id = Column(Integer, primary_key=True, index=True)
    pc_hw_code = Column(String(100), unique=True, index=True)
    pc_name = Column(String(100))
    is_active = Column(Boolean, default=True)
    last_seen = Column(DateTime)
    created_at = Column(DateTime, default=datetime.utcnow)

# Naver Accounts
class NaverAccount(Base):
    __tablename__ = "naver_accounts"

    id = Column(Integer, primary_key=True, index=True)
    naver_id = Column(String(100), unique=True, index=True)
    naver_pw = Column(String(255))
    blog_url = Column(String(255), nullable=True)
    proxy_ip = Column(String(100), nullable=True)
    status = Column(String(50), default="active")  # active, blocked, needs_captcha
    is_active = Column(Boolean, default=True)
    daily_limit = Column(Integer, default=10)  # 일일 작업 제한
    today_count = Column(Integer, default=0)  # 오늘 작업 수
    last_used = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    assignments = relationship("TaskAssignment", back_populates="naver_account")

# Blog Distribution (블로그 배포 - 메인 작업 단위)
class BlogDistribution(Base):
    __tablename__ = "blog_distributions"

    id = Column(Integer, primary_key=True, index=True)

    # 플레이스 정보
    place_url = Column(String(500))  # 네이버 플레이스 URL
    place_name = Column(String(200))
    place_address = Column(String(500))
    place_category = Column(String(100), nullable=True)
    place_phone = Column(String(50), nullable=True)
    place_id = Column(String(100), nullable=True)  # 네이버 플레이스 ID

    # 배포 설정
    distribution_category = Column(String(100))  # 배포 카테고리
    image_option = Column(String(50), default="none")  # none, text_image, pixabay, google_drive, custom
    image_urls = Column(Text, nullable=True)  # JSON array
    google_drive_url = Column(String(500), nullable=True)
    pixabay_keyword = Column(String(200), nullable=True)

    # 스케줄링
    start_date = Column(Date)  # 시작일
    daily_task_count = Column(Integer, default=1)  # 일 작업 수 (1~500)
    total_days = Column(Integer, default=1)  # 작업 일 수
    expected_end_date = Column(Date)  # 예상 종료일

    # 키워드 설정
    main_keyword = Column(String(200))  # 제목 필수 키워드
    sub_keywords = Column(Text, nullable=True)  # 추가 키워드 (JSON)
    forbidden_words = Column(Text, nullable=True)  # 금지어 (쉼표 구분)
    bottom_tags = Column(Text, nullable=True)  # 하단 태그

    # GPT 설정
    prompt_template = Column(Text, nullable=True)  # 커스텀 프롬프트

    # 상태
    status = Column(String(50), default="active")  # active, paused, completed, cancelled
    total_tasks = Column(Integer, default=0)  # 전체 작업 수
    completed_tasks = Column(Integer, default=0)  # 완료된 작업 수
    failed_tasks = Column(Integer, default=0)  # 실패한 작업 수

    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    assignments = relationship("TaskAssignment", back_populates="distribution")

# Task Assignment (개별 작업 할당)
class TaskAssignment(Base):
    __tablename__ = "task_assignments"

    id = Column(Integer, primary_key=True, index=True)
    distribution_id = Column(Integer, ForeignKey("blog_distributions.id"))
    naver_account_id = Column(Integer, ForeignKey("naver_accounts.id"), nullable=True)
    pc_hw_code = Column(String(100), nullable=True)

    # 스케줄
    scheduled_date = Column(Date)  # 예정일

    # 상태
    status = Column(String(50), default="pending")  # pending, in_progress, completed, fail

    # 결과
    post_title = Column(String(500), nullable=True)
    result_post_url = Column(String(500), nullable=True)
    error_message = Column(Text, nullable=True)

    assigned_at = Column(DateTime, nullable=True)
    started_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    distribution = relationship("BlogDistribution", back_populates="assignments")
    naver_account = relationship("NaverAccount", back_populates="assignments")

# Settings
class Setting(Base):
    __tablename__ = "settings"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String(100), unique=True, index=True)
    value = Column(Text)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

# Task History (로그)
class TaskHistory(Base):
    __tablename__ = "task_history"

    id = Column(Integer, primary_key=True, index=True)
    assignment_id = Column(Integer, ForeignKey("task_assignments.id"), nullable=True)
    distribution_id = Column(Integer, ForeignKey("blog_distributions.id"), nullable=True)
    action = Column(String(100))
    details = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

def init_db():
    Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
