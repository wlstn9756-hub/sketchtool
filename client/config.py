"""
블로그 자동화 클라이언트 설정
"""
import os
import json
import uuid
import hashlib
from pathlib import Path

# 기본 설정
BASE_DIR = Path(__file__).parent
CONFIG_FILE = BASE_DIR / "client_config.json"

# 서버 설정
DEFAULT_SERVER_URL = "http://localhost:8000"

def get_hw_code():
    """하드웨어 고유 코드 생성"""
    try:
        # MAC 주소 + 호스트명으로 고유 코드 생성
        mac = uuid.getnode()
        hostname = os.environ.get('COMPUTERNAME', os.environ.get('HOSTNAME', 'unknown'))
        unique_str = f"{mac}-{hostname}"
        return hashlib.md5(unique_str.encode()).hexdigest()[:16].upper()
    except:
        return "UNKNOWN-PC"

def load_config():
    """설정 로드"""
    default_config = {
        "server_url": DEFAULT_SERVER_URL,
        "pc_name": os.environ.get('COMPUTERNAME', os.environ.get('HOSTNAME', 'MyPC')),
        "hw_code": get_hw_code(),
        "chrome_headless": False,
        "task_interval": 60,  # 작업 간 대기 시간 (초)
        "max_retries": 3,
    }

    if CONFIG_FILE.exists():
        try:
            with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
                saved_config = json.load(f)
                default_config.update(saved_config)
        except:
            pass

    return default_config

def save_config(config):
    """설정 저장"""
    with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
        json.dump(config, f, ensure_ascii=False, indent=2)

# 전역 설정
CONFIG = load_config()
