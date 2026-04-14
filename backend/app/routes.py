from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import DiagnosticoJ1939, TransaccionPOS, InventoryEdge, Lead
from app.schemas import (
    BulkSyncRequest,
    BulkSyncResponse,
    DiagnosticoCreate,
    DiagnosticoResponse,
    InventoryCreate,
    InventoryResponse,
    LeadCreate,
    LeadResponse,
    TransaccionCreate,
    TransaccionResponse,
)

router = APIRouter()
security = HTTPBearer(auto_error=False)


def get_api_key(credentials: HTTPAuthorizationCredentials | None = Depends(security)):
    """Extract API key from Authorization header."""
    if credentials and credentials.credentials.startswith("apikey "):
        return credentials.credentials[7:]  # Remove "apikey " prefix
    return None


@router.get("/health")
async def health():
    return {"status": "ok", "service": "smarter-os-core"}


@router.get("/")
async def root():
    return {"service": "SmarterOS Core API", "version": "1.0.0", "endpoints": ["/leads", "/diagnosticos", "/transacciones", "/inventory", "/sync/bulk"]}


# ─── Diagnósticos ────────────────────────────────────────────────────────────

@router.post("/diagnosticos", response_model=DiagnosticoResponse, status_code=201)
async def create_diagnostico(data: DiagnosticoCreate, db: AsyncSession = Depends(get_db)):
    diagnosticos = DiagnosticoJ1939(**data.model_dump())
    db.add(diagnosticos)
    await db.flush()
    await db.refresh(diagnosticos)
    return diagnosticos


@router.get("/diagnosticos/{vin}", response_model=list[DiagnosticoResponse])
async def get_diagnosticos(vin: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(DiagnosticoJ1939)
        .where(DiagnosticoJ1939.vin == vin)
        .order_by(DiagnosticoJ1939.timestamp.desc())
        .limit(50)
    )
    return result.scalars().all()


# ─── Transacciones ───────────────────────────────────────────────────────────

@router.post("/transacciones", response_model=TransaccionResponse, status_code=201)
async def create_transaccion(data: TransaccionCreate, db: AsyncSession = Depends(get_db)):
    transaccion = TransaccionPOS(**data.model_dump())
    db.add(transaccion)
    await db.flush()
    await db.refresh(transaccion)
    return transaccion


@router.get("/transacciones", response_model=list[TransaccionResponse])
async def get_transacciones(comercio_id: str | None = None, db: AsyncSession = Depends(get_db)):
    query = select(TransaccionPOS).order_by(TransaccionPOS.timestamp.desc()).limit(100)
    if comercio_id:
        query = query.where(TransaccionPOS.comercio_id == comercio_id)
    result = await db.execute(query)
    return result.scalars().all()


# ─── Inventory ────────────────────────────────────────────────────────────────

@router.post("/inventory", response_model=InventoryResponse, status_code=201)
async def create_inventory(data: InventoryCreate, db: AsyncSession = Depends(get_db)):
    inventory = InventoryEdge(
        **data.model_dump(),
        total_items=sum(item.get("qty", 0) for item in data.items),
    )
    db.add(inventory)
    await db.flush()
    await db.refresh(inventory)
    return inventory


# ─── Leads ───────────────────────────────────────────────────────────────────

@router.post("/leads", response_model=LeadResponse, status_code=201)
async def create_lead(data: LeadCreate, db: AsyncSession = Depends(get_db)):
    lead = Lead(**data.model_dump())
    db.add(lead)
    await db.flush()
    await db.refresh(lead)
    return lead


@router.get("/leads", response_model=list[LeadResponse])
async def get_leads(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Lead).order_by(Lead.timestamp.desc()).limit(100)
    )
    return result.scalars().all()


# ─── Bulk Sync (para sincronización offline) ─────────────────────────────────

@router.post("/sync/bulk", response_model=BulkSyncResponse)
async def bulk_sync(data: BulkSyncRequest, db: AsyncSession = Depends(get_db)):
    response = BulkSyncResponse(success=True)

    try:
        if data.diagnosticos:
            records = [DiagnosticoJ1939(**d.model_dump()) for d in data.diagnosticos]
            db.add_all(records)
            response.diagnosticos_created = len(records)

        if data.transacciones:
            records = [TransaccionPOS(**t.model_dump()) for t in data.transacciones]
            db.add_all(records)
            response.transacciones_created = len(records)

        if data.inventory:
            records = [
                InventoryEdge(**i.model_dump(), total_items=sum(item.get("qty", 0) for item in i.items))
                for i in data.inventory
            ]
            db.add_all(records)
            response.inventory_created = len(records)

        if data.leads:
            records = [Lead(**l.model_dump()) for l in data.leads]
            db.add_all(records)
            response.leads_created = len(records)

        await db.commit()

    except Exception as e:
        await db.rollback()
        response.success = False
        response.errors.append(str(e))

    return response
