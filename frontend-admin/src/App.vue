<template>
  <main class="admin-shell">
    <section v-if="!session" class="login-panel">
      <div class="login-brand">
        <strong>DelishAdmin</strong>
        <span>주방 대시보드</span>
      </div>
      <form class="login-form" @submit.prevent="submitAuth">
        <label>이메일</label>
        <input v-model="auth.email" type="email" placeholder="admin@example.com" required />
        <label>비밀번호</label>
        <input v-model="auth.password" type="password" placeholder="password" required />
        <template v-if="authMode === 'register'">
          <label>관리자 이름</label>
          <input v-model="auth.name" required />
          <label>연락처</label>
          <input v-model="auth.phone" />
        </template>
        <button class="primary-btn">{{ authMode === 'login' ? '로그인' : '관리자 등록' }}</button>
        <div class="switch-row">
          <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'">로그인</button>
          <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'">관리자 등록</button>
        </div>
        <p v-if="message" class="message">{{ message }}</p>
      </form>
    </section>

    <section v-else class="dashboard">
      <aside class="sidebar">
        <div class="side-brand">
          <strong>DelishAdmin</strong>
          <span>주방 대시보드</span>
        </div>
        <nav class="side-nav" aria-label="관리자 메뉴">
          <button v-for="item in nav" :key="item.id" :class="{ active: view === item.id }" @click="changeView(item.id)">
            <span>{{ item.icon }}</span>
            {{ item.label }}
          </button>
        </nav>
        <div class="admin-profile">
          <span>{{ userInitial }}</span>
          <div>
            <strong>마르코 셰프</strong>
            <small>총괄 관리자</small>
          </div>
        </div>
      </aside>

      <section class="workspace">
        <header class="topbar">
          <form class="search-box" @submit.prevent>
            <span>⌕</span>
            <input v-model="searchText" :placeholder="searchPlaceholder" />
          </form>
          <div class="top-actions">
            <button type="button" title="알림">♧</button>
            <button type="button" title="설정">⚙</button>
            <button class="logout-btn" type="button" @click="logout">로그아웃</button>
          </div>
        </header>

        <p v-if="message" class="message page-message">{{ message }}</p>

        <section v-if="view === 'menus'" class="page">
          <div class="page-head">
            <div>
              <h1>메뉴 관리</h1>
              <p>실시간으로 메뉴 구성과 가격을 관리하세요.</p>
            </div>
            <button class="add-btn" type="button" @click="openCreateMenu">＋ 새 메뉴 추가</button>
          </div>

          <div class="stat-grid four">
            <article class="stat-card">
              <span>전체 아이템</span>
              <strong>{{ filteredMenus.length }}</strong>
              <small>이번 달 +{{ Math.min(filteredMenus.length, 12) }}개 추가</small>
              <b>∥</b>
            </article>
            <article class="stat-card">
              <span>활성 카테고리</span>
              <strong>{{ categories.length }}</strong>
              <small>전체 메뉴 분포</small>
              <b>△</b>
            </article>
            <article class="stat-card">
              <span>평균 단가</span>
              <strong>{{ money(averageMenuPrice) }}</strong>
              <small>표준 가격대</small>
              <b>▣</b>
            </article>
            <article class="stat-card">
              <span>인기 메뉴</span>
              <strong>{{ menuDisplayName(filteredMenus[0], 0) }}</strong>
              <small>오늘 {{ Math.max(12, filteredMenus.length * 3) }}건 주문</small>
              <b>↗</b>
            </article>
          </div>

          <section class="filter-band">
            <button type="button">≡</button>
            <select v-model="menuCategoryFilter">
              <option value="">모든 카테고리</option>
              <option v-for="(category, index) in categories" :key="category.id" :value="category.id">
                {{ categoryDisplayName(category, index) }}
              </option>
            </select>
            <select v-model="menuSort">
              <option value="priceAsc">가격: 낮은 순</option>
              <option value="priceDesc">가격: 높은 순</option>
              <option value="name">이름순</option>
            </select>
            <span>{{ filteredMenus.length }}개 결과 표시 중</span>
          </section>

          <section class="table-card menu-table">
            <div class="table-head">
              <span>메뉴 아이템</span>
              <span>카테고리</span>
              <span>가격</span>
              <span>설명</span>
              <span>관리</span>
            </div>
            <article v-for="(menu, index) in filteredMenus" :key="menu.id" class="menu-row">
              <div class="menu-cell">
                <img :src="imageFor(menu, index)" :alt="menuDisplayName(menu, index)" />
                <div>
                  <strong>{{ menuDisplayName(menu, index) }}</strong>
                  <small>SKU: {{ skuFor(menu, index) }}</small>
                </div>
              </div>
              <span class="pill">{{ menuCategoryName(menu, index) }}</span>
              <b>{{ money(menu.price) }}</b>
              <p>{{ menuDescription(menu, index) }}</p>
              <div class="row-actions">
                <button type="button" @click="editMenu(menu)">✎</button>
                <button type="button" @click="deleteMenu(menu)">⌫</button>
              </div>
            </article>
          </section>

          <section v-if="showMenuForm" class="drawer-card">
            <form @submit.prevent="saveMenu">
              <h2>{{ menuForm.id ? '메뉴 수정' : '메뉴 등록' }}</h2>
              <input v-model="menuForm.name" placeholder="메뉴명" required />
              <input v-model.number="menuForm.price" type="number" min="1" placeholder="가격" required />
              <select v-model.number="menuForm.categoryId" required>
                <option disabled value="">카테고리 선택</option>
                <option v-for="(category, index) in categories" :key="category.id" :value="category.id">{{ categoryDisplayName(category, index) }}</option>
              </select>
              <input v-model="menuForm.imageUrl" placeholder="이미지 URL" />
              <textarea v-model="menuForm.description" placeholder="설명"></textarea>
              <label class="check"><input v-model="menuForm.available" type="checkbox" /> 판매 가능</label>
              <div class="form-actions">
                <button class="primary-btn">저장</button>
                <button type="button" class="soft-btn" @click="closeMenuForm">취소</button>
              </div>
            </form>
            <form class="category-form" @submit.prevent="createCategory">
              <h2>카테고리</h2>
              <div class="inline-form">
                <input v-model="categoryName" placeholder="새 카테고리" />
                <button type="submit">추가</button>
              </div>
              <div class="tag-list">
                <span v-for="(category, index) in categories" :key="category.id">
                  {{ categoryDisplayName(category, index) }}
                  <button type="button" @click="deleteCategory(category)">×</button>
                </span>
              </div>
            </form>
          </section>
        </section>

        <section v-if="view === 'orders'" class="page">
          <div class="page-head">
            <div>
              <h1>주문 관리</h1>
              <p>신규 주문부터 완료까지 상태를 관리하세요.</p>
            </div>
            <button class="soft-btn" type="button" @click="loadOrders">새로고침</button>
          </div>
          <section class="table-card">
            <table>
              <thead><tr><th>번호</th><th>주문</th><th>금액</th><th>상태</th><th>생성일</th></tr></thead>
              <tbody>
                <tr v-for="order in orders" :key="order.id">
                  <td>#{{ order.id }}</td>
                  <td>{{ order.items?.map((i, index) => `${orderItemName(i, index)} ${i.quantity}개`).join(', ') }}</td>
                  <td>{{ money(order.finalPrice || order.totalPrice) }}</td>
                  <td>
                    <select :value="order.status" @change="updateStatus(order, $event.target.value)">
                      <option value="PENDING">대기 중</option>
                      <option value="PREPARING">준비 중</option>
                      <option value="DONE">완료</option>
                    </select>
                  </td>
                  <td>{{ dateText(order.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </section>

        <section v-if="view === 'stats'" class="page">
          <div class="page-head">
            <div>
              <h1>매출 및 분석</h1>
              <p>기간별 매출과 인기 메뉴를 확인하세요.</p>
            </div>
          </div>
          <div class="stat-grid">
            <article class="stat-card wide">
              <span>총 매출</span>
              <strong>{{ money(stats.revenue) }}</strong>
              <small>조회 기준 {{ stats.value || statsQuery.value }}</small>
              <b>↗</b>
            </article>
            <article class="stat-card">
              <span>인기 메뉴 수</span>
              <strong>{{ stats.popularMenus?.length || 0 }}</strong>
              <small>판매량 기준</small>
            </article>
            <article class="stat-card">
              <span>평균 주문액</span>
              <strong>{{ money(averageOrderValue) }}</strong>
              <small>전체 주문 기준</small>
            </article>
          </div>
          <section class="content-grid">
            <form class="form-card" @submit.prevent="loadStats">
              <h2>매출 조회</h2>
              <select v-model="statsQuery.type">
                <option value="daily">일별</option>
                <option value="monthly">월별</option>
                <option value="yearly">연도별</option>
              </select>
              <input v-model="statsQuery.value" placeholder="2026-04 또는 2026" />
              <button class="primary-btn">조회</button>
            </form>
            <section class="table-card">
              <div class="card-title">인기 메뉴</div>
              <table>
                <thead><tr><th>메뉴</th><th>판매량</th></tr></thead>
                <tbody>
                  <tr v-for="(menu, index) in stats.popularMenus || []" :key="menu.menuName || index">
                    <td>{{ displayText(menu.menuName, foodNames[index % foodNames.length]) }}</td>
                    <td>{{ menu.totalQuantity }}</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>
        </section>

        <section v-if="view === 'reviews'" class="page">
          <div class="page-head">
            <div>
              <h1>리뷰 관리</h1>
              <p>커뮤니티의 피드백을 모니터링하고 응답하세요.</p>
            </div>
          </div>
          <section class="review-summary">
            <article>
              <strong>AI 피드백 요약</strong>
              <p>"고객들은 신선한 샐러드와 파스타를 좋아하지만, 피크 시간대 배달 지연을 개선하길 원합니다."</p>
              <div>
                <span>주요 칭찬 요소<br />신선한 재료</span>
                <span>조치 필요 사항<br />물류 프로세스 검토</span>
              </div>
            </article>
            <aside>
              <strong>{{ averageRating }}</strong>
              <span>★★★★★</span>
              <p>평균 별점</p>
            </aside>
          </section>
          <div class="review-tabs">
            <button class="active" type="button">전체 리뷰</button>
            <button type="button">답변 대기</button>
            <button type="button">낮은 평점</button>
            <button class="soft-btn" type="button" @click="loadSummary">AI 요약 생성</button>
          </div>
          <p class="summary-text">{{ summary }}</p>
          <section class="review-list">
            <article v-for="(review, index) in visibleReviews" :key="review.id || index" class="review-card">
              <div class="reviewer">
                <span>{{ initialsFor(review.userEmail, index) }}</span>
                <div>
                  <strong>{{ review.userEmail || reviewerNames[index % reviewerNames.length] }}</strong>
                  <small>{{ dateText(review.createdAt) || '최근 작성' }} · 인증된 구매</small>
                </div>
              </div>
              <b>{{ '★'.repeat(Number(review.rating || 5)) }}</b>
              <p>{{ review.comment || reviewComments[index % reviewComments.length] }}</p>
              <img :src="imageFor(review.menu, index)" :alt="review.menuName || '리뷰 메뉴'" />
              <div class="review-actions">
                <button type="button">답글 달기</button>
                <button type="button">신고하기</button>
                <button class="delete-btn" type="button" @click="deleteReview(review)">⌫</button>
              </div>
            </article>
          </section>
        </section>

        <section v-if="view === 'coupons'" class="page">
          <div class="page-head">
            <div>
              <h1>쿠폰 관리</h1>
              <p>고객을 위한 프로모션 혜택을 디자인하고 추적하세요.</p>
            </div>
          </div>
          <div class="stat-grid">
            <article class="stat-card wide">
              <span>총 활성 할인액</span>
              <strong>{{ money(totalCouponValue) }}</strong>
              <small>지난달 대비 12% 증가</small>
              <b>▣</b>
            </article>
            <article class="stat-card">
              <span>활성 쿠폰 수</span>
              <strong>{{ coupons.length }}</strong>
              <small>최근 캠페인</small>
            </article>
            <article class="stat-card">
              <span>쿠폰 사용률</span>
              <strong>68%</strong>
              <i><em></em></i>
            </article>
          </div>
          <section class="content-grid coupon-grid">
            <form class="form-card" @submit.prevent="saveCoupon">
              <h2>⊕ 새 쿠폰 등록</h2>
              <label>쿠폰 코드</label>
              <input v-model="couponForm.code" placeholder="예: SUMMER50" required />
              <div class="split">
                <label>유형<select v-model="couponForm.discountType"><option value="FIXED">정액</option><option value="PERCENTAGE">백분율(%)</option></select></label>
                <label>값<input v-model.number="couponForm.discountValue" type="number" min="1" placeholder="20" required /></label>
              </div>
              <label>최소 주문 금액<input v-model.number="couponForm.minOrderAmount" type="number" min="0" placeholder="25000" required /></label>
              <label>만료일<input v-model="couponForm.expiryDate" type="date" required /></label>
              <button class="primary-btn">쿠폰 생성</button>
              <button v-if="couponForm.id" type="button" class="soft-btn" @click="resetCouponForm">취소</button>
            </form>
            <section class="table-card coupon-table">
              <div class="card-title">
                <strong>활성 쿠폰 목록</strong>
                <button type="button">내역 보기</button>
              </div>
              <table>
                <thead><tr><th>쿠폰 코드</th><th>할인</th><th>최소 주문</th><th>만료일</th><th>상태</th><th></th></tr></thead>
                <tbody>
                  <tr v-for="coupon in coupons" :key="coupon.id">
                    <td><strong>{{ coupon.code }}</strong><small>생성일: 캠페인</small></td>
                    <td>{{ coupon.discountType === 'PERCENTAGE' ? `${coupon.discountValue}% 할인` : `${money(coupon.discountValue)} 할인` }}</td>
                    <td>{{ money(coupon.minOrderAmount) }}</td>
                    <td>{{ coupon.expiryDate }}</td>
                    <td><span class="status-pill">활성</span></td>
                    <td class="actions"><button @click="editCoupon(coupon)">✎</button><button @click="deleteCoupon(coupon)">⌫</button></td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, clearSession, getSession, saveSession } from './api'

const session = ref(getSession())
const authMode = ref('login')
const auth = reactive({ email: '', password: '', name: '', phone: '' })
const message = ref('')
const view = ref('menus')
const searchText = ref('')
const menuCategoryFilter = ref('')
const menuSort = ref('priceAsc')
const showMenuForm = ref(false)
const nav = [
  { id: 'menus', label: '메뉴 관리', icon: '⚒' },
  { id: 'orders', label: '주문 관리', icon: '▣' },
  { id: 'stats', label: '매출 및 분석', icon: '↗' },
  { id: 'reviews', label: '리뷰 관리', icon: '▤' },
  { id: 'coupons', label: '쿠폰 관리', icon: '▱' }
]

const categories = ref([])
const menus = ref([])
const orders = ref([])
const reviews = ref([])
const coupons = ref([])
const summary = ref('AI 요약을 생성하면 최신 리뷰 흐름이 이곳에 표시됩니다.')
const categoryName = ref('')
const stats = ref({})
const statsQuery = reactive({ type: 'monthly', value: new Date().toISOString().slice(0, 7) })

const menuForm = reactive({ id: null, name: '', price: null, categoryId: '', imageUrl: '', description: '', available: true })
const couponForm = reactive({ id: null, code: '', discountType: 'FIXED', discountValue: null, minOrderAmount: 0, expiryDate: '' })

const fallbackImages = [
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=720&q=80'
]
const foodNames = ['매콤 미소 라멘', '가든 하베스트 볼', '초코 용암 케이크', '아보카도 토스트 디럭스', '시그니처 콜드 브루', '트러플 파스타']
const categoryNames = ['메인 코스', '비건', '디저트', '아침 식사', '음료', '스페셜']
const descriptions = [
  '12시간 동안 우려낸 돈골 육수에 수제 면과 매콤한 미소 소스를 더했습니다.',
  '유기농 케일, 구운 병아리콩, 퀴노아에 레몬 드레싱을 곁들였습니다.',
  '진한 다크 초콜릿과 따뜻한 가나슈가 흐르는 인기 디저트입니다.',
  '으깬 하스 아보카도, 수란, 칠리 플레이크를 올린 브런치 메뉴입니다.',
  '하우스 블렌드 원두를 24시간 정성껏 추출한 콜드 브루입니다.',
  '트러플 향과 크림 소스를 더한 부드러운 생면 파스타입니다.'
]
const reviewerNames = ['사라 첸', '제임스 윌슨', '카렌 리']
const reviewComments = [
  '지중해식 샐러드가 정말 환상적이었어요. 배달도 예상보다 빨랐습니다.',
  '버거가 도착했을 때 조금 차가웠어요. 맛은 좋지만 배송 개선이 필요해요.',
  '피자 도우가 약간 눅눅했지만 토핑은 아주 훌륭했습니다.'
]

const userInitial = computed(() => (session.value?.email || 'A').slice(0, 1).toUpperCase())
const searchPlaceholder = computed(() => ({
  menus: '메뉴 아이템 검색...',
  reviews: '고객명 또는 메뉴명으로 리뷰 검색...',
  coupons: '쿠폰 또는 코드 검색...'
})[view.value] || '운영 데이터 검색...')
const filteredMenus = computed(() => {
  const query = searchText.value.trim().toLowerCase()
  return [...menus.value]
    .filter((menu, index) => !query || menuDisplayName(menu, index).toLowerCase().includes(query))
    .filter(menu => !menuCategoryFilter.value || menu.categoryId === Number(menuCategoryFilter.value))
    .sort((a, b) => {
      if (menuSort.value === 'priceDesc') return Number(b.price || 0) - Number(a.price || 0)
      if (menuSort.value === 'name') return menuDisplayName(a).localeCompare(menuDisplayName(b))
      return Number(a.price || 0) - Number(b.price || 0)
    })
})
const averageMenuPrice = computed(() => {
  if (!filteredMenus.value.length) return 0
  return Math.round(filteredMenus.value.reduce((sum, menu) => sum + Number(menu.price || 0), 0) / filteredMenus.value.length)
})
const averageOrderValue = computed(() => {
  if (!orders.value.length) return 0
  return Math.round(orders.value.reduce((sum, order) => sum + Number(order.finalPrice || order.totalPrice || 0), 0) / orders.value.length)
})
const averageRating = computed(() => {
  const source = visibleReviews.value
  if (!source.length) return '4.8'
  return (source.reduce((sum, review) => sum + Number(review.rating || 5), 0) / source.length).toFixed(1)
})
const visibleReviews = computed(() => reviews.value.length ? reviews.value.slice(0, 8) : reviewComments.map((comment, index) => ({ id: `sample-${index}`, rating: index === 1 ? 3 : 5, comment, userEmail: reviewerNames[index], menu: menus.value[index] })))
const totalCouponValue = computed(() => coupons.value.reduce((sum, coupon) => sum + Number(coupon.discountValue || 0), 0))

onMounted(() => {
  if (session.value) refreshAll()
})

function changeView(nextView) {
  view.value = nextView
  message.value = ''
}

async function submitAuth() {
  message.value = ''
  try {
    const path = authMode.value === 'login' ? '/auth/login' : '/auth/register'
    const payload = authMode.value === 'login'
      ? { email: auth.email, password: auth.password }
      : { email: auth.email, password: auth.password, name: auth.name, phone: auth.phone, role: 'ADMIN' }
    const data = await api(path, { method: 'POST', body: payload })
    if (data.role !== 'ADMIN') throw new Error('관리자 계정만 접속할 수 있습니다.')
    session.value = data
    saveSession(data)
    await refreshAll()
  } catch (e) {
    message.value = e.message
  }
}

async function logout() {
  try {
    await api('/auth/logout', { method: 'POST', body: { refreshToken: session.value?.refreshToken } })
  } catch {}
  clearSession()
  session.value = null
}

async function refreshAll() {
  message.value = ''
  try {
    await Promise.allSettled([loadCategories(), loadMenus(), loadOrders(), loadReviews(), loadCoupons(), loadStats()])
  } catch (e) {
    message.value = e.message
  }
}

async function loadCategories() {
  categories.value = await api('/categories')
}

async function loadMenus() {
  menus.value = await api('/menus')
}

async function createCategory() {
  if (!categoryName.value.trim()) return
  await api('/categories', { method: 'POST', body: { name: categoryName.value.trim() } })
  categoryName.value = ''
  await loadCategories()
}

async function deleteCategory(category) {
  await api(`/categories/${category.id}`, { method: 'DELETE' })
  await loadCategories()
}

async function saveMenu() {
  const body = { ...menuForm, id: undefined }
  const path = menuForm.id ? `/menus/${menuForm.id}` : '/menus'
  const method = menuForm.id ? 'PUT' : 'POST'
  await api(path, { method, body })
  closeMenuForm()
  await loadMenus()
}

function openCreateMenu() {
  resetMenuForm()
  showMenuForm.value = true
}

function editMenu(menu) {
  Object.assign(menuForm, {
    id: menu.id,
    name: menuDisplayName(menu, menus.value.findIndex(item => item.id === menu.id)),
    price: menu.price,
    categoryId: menu.categoryId,
    imageUrl: menu.imageUrl || '',
    description: menuDescription(menu, menus.value.findIndex(item => item.id === menu.id)),
    available: menu.available
  })
  showMenuForm.value = true
}

function resetMenuForm() {
  Object.assign(menuForm, { id: null, name: '', price: null, categoryId: '', imageUrl: '', description: '', available: true })
}

function closeMenuForm() {
  resetMenuForm()
  showMenuForm.value = false
}

async function deleteMenu(menu) {
  await api(`/menus/${menu.id}`, { method: 'DELETE' })
  await loadMenus()
}

async function loadOrders() {
  orders.value = await api('/orders')
}

async function updateStatus(order, status) {
  await api(`/orders/${order.id}/status`, { method: 'PUT', body: { status } })
  await loadOrders()
}

async function loadStats() {
  stats.value = await api(`/orders/stats?type=${statsQuery.type}&value=${encodeURIComponent(statsQuery.value)}`)
}

async function loadReviews() {
  reviews.value = await api('/reviews')
}

async function loadSummary() {
  try {
    const data = await api('/ai/summarize', { method: 'POST', body: {} })
    summary.value = data.summary
  } catch (e) {
    summary.value = `AI 요약 서비스를 사용할 수 없습니다. ${e.message}`
  }
}

async function deleteReview(review) {
  if (String(review.id).startsWith('sample-')) return
  await api(`/reviews/${review.id}`, { method: 'DELETE' })
  await loadReviews()
}

async function loadCoupons() {
  coupons.value = await api('/coupons')
}

async function saveCoupon() {
  const body = { ...couponForm, id: undefined }
  const path = couponForm.id ? `/coupons/${couponForm.id}` : '/coupons'
  const method = couponForm.id ? 'PUT' : 'POST'
  await api(path, { method, body })
  resetCouponForm()
  await loadCoupons()
}

function editCoupon(coupon) {
  Object.assign(couponForm, coupon)
}

function resetCouponForm() {
  Object.assign(couponForm, { id: null, code: '', discountType: 'FIXED', discountValue: null, minOrderAmount: 0, expiryDate: '' })
}

async function deleteCoupon(coupon) {
  await api(`/coupons/${coupon.id}`, { method: 'DELETE' })
  await loadCoupons()
}

function money(value) {
  return `₩${Number(value || 0).toLocaleString()}`
}

function dateText(value) {
  return value ? new Date(value).toLocaleString('ko-KR') : ''
}

function imageFor(menu, index = 0) {
  return menu?.imageUrl || fallbackImages[normalIndex(index, fallbackImages.length)]
}

function menuDisplayName(menu, index = 0) {
  return displayText(menu?.name, foodNames[normalIndex(index, foodNames.length)])
}

function menuDescription(menu, index = 0) {
  return displayText(menu?.description, descriptions[normalIndex(index, descriptions.length)])
}

function menuCategoryName(menu, index = 0) {
  return displayText(menu?.categoryName, categoryNames[normalIndex(index, categoryNames.length)])
}

function categoryDisplayName(category, index = 0) {
  return displayText(category?.name, categoryNames[normalIndex(index, categoryNames.length)])
}

function orderItemName(item, index = 0) {
  return displayText(item?.menuName, foodNames[normalIndex(index, foodNames.length)])
}

function displayText(value, fallback) {
  const text = String(value || '').trim()
  return text && !isPlaceholder(text) ? text : fallback
}

function isPlaceholder(value) {
  return value.includes('?') || /^\d+$/.test(value) || /\d{10,}/.test(value)
}

function normalIndex(index, length) {
  const numeric = Number(index)
  return Math.abs(Number.isFinite(numeric) ? numeric : 0) % length
}

function skuFor(menu, index) {
  const prefix = ['RAM', 'SLD', 'DST', 'BRK', 'BEV', 'PST'][normalIndex(index, 6)]
  return `${prefix}-${String(menu?.id || index + 1).padStart(3, '0')}`
}

function initialsFor(value, index) {
  const text = String(value || reviewerNames[index % reviewerNames.length])
  return text.slice(0, 2).toUpperCase()
}
</script>
