import os
import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import anthropic
from typing import Optional
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

client = anthropic.Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))

ORDER_SERVICE_URL = os.getenv("ORDER_SERVICE_URL", "http://order-service:8083")
REVIEW_SERVICE_URL = os.getenv("REVIEW_SERVICE_URL", "http://review-service:8084")


class SummarizeRequest(BaseModel):
    menu_id: Optional[int] = None


@app.get("/ai/recommend")
async def recommend():
    popular_menus = []
    try:
        async with httpx.AsyncClient(timeout=5.0) as c:
            res = await c.get(
                f"{ORDER_SERVICE_URL}/orders/stats",
                params={"type": "monthly", "value": "2025-01"},
                headers={"X-User-Role": "ADMIN"},
            )
            if res.status_code == 200:
                data = res.json()
                popular_menus = data.get("popularMenus", [])
    except Exception as e:
        logger.warning(f"Failed to fetch order stats: {e}")

    context = ""
    if popular_menus:
        top = ", ".join([m["menuName"] for m in popular_menus[:5]])
        context = f"최근 인기 메뉴: {top}."

    prompt = f"""당신은 음식 추천 전문가입니다. {context}
시간대별 음식을 추천해주세요. 아래 형식으로 정확히 3개만 출력하세요:
- 아침엔 [음식명] 어때요?
- 점심엔 [음식명] 어때요?
- 저녁엔 [음식명] 어때요?"""

    try:
        response = client.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=200,
            messages=[{"role": "user", "content": prompt}],
        )
        text = response.content[0].text.strip()
        recommendations = [line.strip("- ").strip() for line in text.split("\n") if line.strip()]
        return {"recommendations": recommendations}
    except Exception as e:
        logger.error(f"Claude API error: {e}")
        return {
            "recommendations": [
                "아침엔 토스트 어때요?",
                "점심엔 비빔밥 어때요?",
                "저녁엔 삼겹살 어때요?",
            ]
        }


@app.post("/ai/summarize")
async def summarize(req: SummarizeRequest):
    reviews = []
    try:
        async with httpx.AsyncClient(timeout=5.0) as c:
            params = {}
            if req.menu_id:
                params["menuId"] = req.menu_id
            res = await c.get(f"{REVIEW_SERVICE_URL}/reviews", params=params)
            if res.status_code == 200:
                reviews = res.json()
    except Exception as e:
        logger.warning(f"Failed to fetch reviews: {e}")
        raise HTTPException(status_code=503, detail="리뷰 서비스에 연결할 수 없습니다.")

    if not reviews:
        return {"summary": "아직 등록된 리뷰가 없습니다."}

    review_texts = "\n".join(
        [f"- 별점 {r['rating']}/5: {r['comment']}" for r in reviews[:50]]
    )
    prompt = f"""다음은 고객 리뷰 목록입니다. 전반적인 평가를 3~5문장으로 요약해주세요.

리뷰:
{review_texts}

요약:"""

    try:
        response = client.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=300,
            messages=[{"role": "user", "content": prompt}],
        )
        summary = response.content[0].text.strip()
        return {"summary": summary}
    except Exception as e:
        logger.error(f"Claude API error: {e}")
        raise HTTPException(status_code=500, detail="AI 요약 생성에 실패했습니다.")


@app.get("/ai/health")
async def health():
    return {"status": "ok"}
