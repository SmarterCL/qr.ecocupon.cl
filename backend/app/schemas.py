from datetime import datetime
from typing import Optional

from pydantic import BaseModel


# ─── Diagnósticos J1939 ──────────────────────────────────────────────────────

class DiagnosticoCreate(BaseModel):
    vin: str
    odometer_km: Optional[float] = None
    engine_hours: Optional[float] = None
    fuel_level_pct: Optional[float] = None
    coolant_temp_c: Optional[float] = None
    oil_pressure_kpa: Optional[float] = None
    rpm: Optional[int] = None
    speed_kmh: Optional[float] = None
    dtc_codes: Optional[list[str]] = None
    dtc_raw: Optional[str] = None
    pgn_data: Optional[dict] = None
    source: str = "app_obd2"


class DiagnosticoResponse(DiagnosticoCreate):
    id: int
    timestamp: datetime

    model_config = {"from_attributes": True}


# ─── Transacciones POS ───────────────────────────────────────────────────────

class TransaccionCreate(BaseModel):
    comercio_id: str
    nfc_tag_id: Optional[str] = None
    monto: float
    sku: Optional[str] = None
    producto: Optional[str] = None
    cantidad: int = 1
    firma_digital_offline: Optional[str] = None
    latitud: Optional[float] = None
    longitud: Optional[float] = None
    source: str = "app_pos"


class TransaccionResponse(TransaccionCreate):
    id: int
    timestamp: datetime
    firma_validada: bool

    model_config = {"from_attributes": True}


# ─── Inventory ────────────────────────────────────────────────────────────────

class InventoryCreate(BaseModel):
    vin: str
    items: list[dict]
    ruta_actual: Optional[str] = None
    parada_numero: Optional[int] = None


class InventoryResponse(InventoryCreate):
    id: int
    timestamp: datetime
    total_items: int

    model_config = {"from_attributes": True}


# ─── Leads ────────────────────────────────────────────────────────────────────

class LeadCreate(BaseModel):
    nombre: str
    telefono: str
    email: Optional[str] = None
    vehiculo_marca: Optional[str] = None
    vehiculo_modelo: Optional[str] = None
    vehiculo_anio: Optional[str] = None
    vehiculo_patente: Optional[str] = None
    dtc_codes: Optional[list[str]] = None
    dtc_raw: Optional[str] = None
    source: str = "app_obd2"


class LeadResponse(LeadCreate):
    id: int
    timestamp: datetime

    model_config = {"from_attributes": True}


# ─── Bulk Sync ────────────────────────────────────────────────────────────────

class BulkSyncRequest(BaseModel):
    diagnosticos: Optional[list[DiagnosticoCreate]] = None
    transacciones: Optional[list[TransaccionCreate]] = None
    inventory: Optional[list[InventoryCreate]] = None
    leads: Optional[list[LeadCreate]] = None


class BulkSyncResponse(BaseModel):
    success: bool
    diagnosticos_created: int = 0
    transacciones_created: int = 0
    inventory_created: int = 0
    leads_created: int = 0
    errors: list[str] = []
