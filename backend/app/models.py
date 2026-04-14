from sqlalchemy import Column, Integer, String, Float, Text, DateTime, JSON, ForeignKey, Boolean
from sqlalchemy.sql import func

from app.database import Base


# ─── Diagnósticos Mercedes (J1939) ───────────────────────────────────────────

class DiagnosticoJ1939(Base):
    __tablename__ = "diagnosticos_j1939"

    id = Column(Integer, primary_key=True, index=True)
    vin = Column(String(17), nullable=False, index=True)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())

    # Telemetry
    odometer_km = Column(Float, nullable=True)
    engine_hours = Column(Float, nullable=True)
    fuel_level_pct = Column(Float, nullable=True)
    coolant_temp_c = Column(Float, nullable=True)
    oil_pressure_kpa = Column(Float, nullable=True)
    rpm = Column(Integer, nullable=True)
    speed_kmh = Column(Float, nullable=True)

    # DTC Codes
    dtc_codes = Column(JSON, nullable=True)  # List of DTC codes
    dtc_raw = Column(Text, nullable=True)

    # PGN Data
    pgn_data = Column(JSON, nullable=True)  # Raw PGN packets

    # Sync metadata
    synced_at = Column(DateTime(timezone=True), server_default=func.now())
    source = Column(String(50), default="app_obd2")


# ─── Transacciones POS (NFC/Offline) ─────────────────────────────────────────

class TransaccionPOS(Base):
    __tablename__ = "transacciones_pos"

    id = Column(Integer, primary_key=True, index=True)
    comercio_id = Column(String(50), nullable=False, index=True)
    nfc_tag_id = Column(String(50), nullable=True, index=True)

    timestamp = Column(DateTime(timezone=True), server_default=func.now())

    # Transaction data
    monto = Column(Float, nullable=False)
    sku = Column(String(100), nullable=True)
    producto = Column(String(200), nullable=True)
    cantidad = Column(Integer, default=1)

    # Offline signature
    firma_digital_offline = Column(Text, nullable=True)
    firma_validada = Column(Boolean, default=False)

    # Location
    latitud = Column(Float, nullable=True)
    longitud = Column(Float, nullable=True)

    # Sync metadata
    synced_at = Column(DateTime(timezone=True), server_default=func.now())
    source = Column(String(50), default="app_pos")


# ─── Inventario del camión ───────────────────────────────────────────────────

class InventoryEdge(Base):
    __tablename__ = "inventory_edge"

    id = Column(Integer, primary_key=True, index=True)
    vin = Column(String(17), nullable=False, index=True)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())

    # Inventory snapshot
    items = Column(JSON, nullable=False)  # [{sku, qty, location}]
    total_items = Column(Integer, default=0)

    # Route
    ruta_actual = Column(String(100), nullable=True)
    parada_numero = Column(Integer, nullable=True)

    # Sync metadata
    synced_at = Column(DateTime(timezone=True), server_default=func.now())


# ─── Leads (desde la app Android OBD2) ───────────────────────────────────────

class Lead(Base):
    __tablename__ = "leads"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(200), nullable=False)
    telefono = Column(String(50), nullable=False)
    email = Column(String(200), nullable=True)

    vehiculo_marca = Column(String(100), nullable=True)
    vehiculo_modelo = Column(String(100), nullable=True)
    vehiculo_anio = Column(String(10), nullable=True)
    vehiculo_patente = Column(String(20), nullable=True)

    dtc_codes = Column(JSON, nullable=True)
    dtc_raw = Column(Text, nullable=True)

    timestamp = Column(DateTime(timezone=True), server_default=func.now())
    synced_at = Column(DateTime(timezone=True), server_default=func.now())
    source = Column(String(50), default="app_obd2")
    estado = Column(String(50), default="nuevo")
