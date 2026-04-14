import hashlib
import hmac
import time
from datetime import datetime, timedelta, timezone

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# API keys for devices (in production, store these in a DB)
VALID_API_KEYS = {
    "ecocupon-android-v1": {
        "name": "Ecocupon Android APK",
        "created": "2026-04-14",
    }
}


def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + (expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm="HS256")


def verify_api_key(api_key: str) -> dict | None:
    """Verify API key and return device info or None."""
    if api_key in VALID_API_KEYS:
        return VALID_API_KEYS[api_key]
    return None


def generate_device_token(device_id: str) -> str:
    """Generate a persistent token for a device."""
    return create_access_token({"device_id": device_id, "type": "device"})


def verify_device_token(token: str) -> dict | None:
    """Verify a device JWT token."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        if payload.get("type") != "device":
            return None
        return payload
    except JWTError:
        return None
