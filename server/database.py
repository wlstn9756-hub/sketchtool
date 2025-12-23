from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime, Boolean, ForeignKey, Enum
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
from datetime import datetime
import enum

SQLALCHEMY_DATABASE_URL = "sqlite:///./blog_auto.db"

engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class TaskStatus(str, enum.Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "fail"
    DELETED = "deleted"

class ImageOption(str, enum.Enum):
    NONE = "none"
    TEXT_IMAGE = "text_image"
    PIXABAY = "pixabay"
    GOOGLE_DRIVE = "google_drive"
    CUSTOM = "custom"

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
    blog_url = Column(String(255))
    proxy_ip = Column(String(100), nullable=True)
    status = Column(String(50), default="active")  # active, blocked, needs_captcha
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    tasks = relationship("BlogTask", back_populates="naver_account")

# Places (Businesses)
class Place(Base):
    __tablename__ = "places"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200))
    address = Column(String(500))
    origin_address = Column(String(500), nullable=True)
    url = Column(String(500), nullable=True)
    category = Column(String(100), nullable=True)
    main_keyword = Column(String(200))
    bottom_tags = Column(Text, nullable=True)  # comma separated
    forbidden_words = Column(Text, nullable=True)  # comma separated
    prompt = Column(Text, nullable=True)
    image_option = Column(String(50), default="none")
    image_urls = Column(Text, nullable=True)  # JSON array
    google_drive_url = Column(String(500), nullable=True)
    pixabay_keyword = Column(String(200), nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    tasks = relationship("BlogTask", back_populates="place")

# Blog Tasks
class BlogTask(Base):
    __tablename__ = "blog_tasks"

    id = Column(Integer, primary_key=True, index=True)
    place_id = Column(Integer, ForeignKey("places.id"))
    naver_account_id = Column(Integer, ForeignKey("naver_accounts.id"), nullable=True)
    pc_hw_code = Column(String(100), nullable=True)

    status = Column(String(50), default="pending")  # pending, in_progress, completed, fail
    task_count = Column(Integer, default=1)
    post_title = Column(String(500), nullable=True)
    result_post_url = Column(String(500), nullable=True)

    assigned_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    place = relationship("Place", back_populates="tasks")
    naver_account = relationship("NaverAccount", back_populates="tasks")

# Settings
class Setting(Base):
    __tablename__ = "settings"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String(100), unique=True, index=True)
    value = Column(Text)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

# Task History
class TaskHistory(Base):
    __tablename__ = "task_history"

    id = Column(Integer, primary_key=True, index=True)
    task_id = Column(Integer, ForeignKey("blog_tasks.id"))
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
