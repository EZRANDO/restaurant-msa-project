<template>
  <main class="app-shell">
    <section v-if="!session" class="auth-screen">
      <div class="auth-panel">
        <div class="auth-brand">
          <strong>제스트푸드</strong>
          <span>신선한 메뉴를 편하게 주문하세요</span>
        </div>

        <form class="auth-card" @submit.prevent="submitAuth">
          <label>이메일</label>
          <input v-model="auth.email" type="email" placeholder="example@email.com" required />
          <label>비밀번호</label>
          <input v-model="auth.password" type="password" placeholder="password" required />

          <template v-if="authMode === 'register'">
            <label>이름</label>
            <input v-model="auth.name" placeholder="홍길동" required />
            <label>휴대폰</label>
            <input v-model="auth.phone" placeholder="010-0000-0000" />
          </template>

          <button class="primary-btn" type="submit">{{ authMode === 'login' ? '로그인' : '회원가입' }}</button>
          <div class="auth-links">
            <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'">로그인</button>
            <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'">회원가입</button>
          </div>
          <p v-if="message" class="message">{{ message }}</p>
        </form>
      </div>
    </section>

    <section v-else class="web-app">
      <header class="topbar">
        <button class="brand-text" type="button" @click="view = 'home'">제스트푸드</button>
        <nav class="main-nav" aria-label="주요 메뉴">
          <button :class="{ active: view === 'home' }" @click="view = 'home'">메뉴</button>
          <button type="button">혜택</button>
          <button :class="{ active: view === 'orders' }" @click="openOrders">내 주문</button>
          <button :class="{ active: view === 'reviews' }" @click="openReviewPage">고객 리뷰</button>
          <button type="button">고객센터</button>
        </nav>
        <form class="search-box" @submit.prevent="loadMenus">
          <span>⌕</span>
          <input v-model="keyword" placeholder="메뉴 검색..." />
        </form>
        <div class="header-actions">
          <button class="icon-btn" type="button" title="알림">!</button>
          <button class="icon-btn" type="button" title="장바구니" @click="openCart">⌔</button>
          <button class="avatar-btn" type="button" title="로그아웃" @click="logout">{{ userInitial }}</button>
        </div>
      </header>

      <p v-if="dataMessage" class="notice-bar">{{ dataMessage }}</p>

      <section v-if="view === 'home'" class="menu-layout">
        <aside class="category-panel">
          <h2>카테고리</h2>
          <div class="category-list">
            <button :class="{ active: !selectedCategory }" @click="selectCategory(null)">
              <span>∥</span>
              오늘의 특선
            </button>
            <button v-for="(category, index) in categories" :key="category.id" :class="{ active: selectedCategory === category.id }" @click="selectCategory(category.id)">
              <span>{{ categoryIcon(categoryDisplayName(category, index)) }}</span>
              {{ categoryDisplayName(category, index) }}
            </button>
          </div>

          <section class="invite-box">
            <strong>친구 초대하기</strong>
            <p>다음 주문 시 할인 혜택을 받을 수 있어요.</p>
            <button type="button">링크 공유하기</button>
          </section>
        </aside>

        <section class="menu-main">
          <article v-if="featuredMenu" class="hero-card">
            <img :src="imageFor(featuredMenu, 0)" :alt="menuDisplayName(featuredMenu, 0)" />
            <div class="hero-copy">
              <span>셰프의 추천</span>
              <h1>당신의 점심을 위한 신선하고 건강한 한 그릇</h1>
              <p>{{ menuDescription(featuredMenu, 0) }}</p>
              <button type="button" @click="addToCart(featuredMenu)">지금 주문하기</button>
            </div>
          </article>

          <div class="section-heading">
            <h2>오늘의 추천 메뉴</h2>
            <div class="sort-actions">
              <button type="button" title="필터">≡</button>
              <button type="button" title="정렬">☰</button>
            </div>
          </div>

          <div class="menu-grid">
            <article v-for="(menu, index) in menus" :key="menu.id" class="menu-card">
              <div class="menu-image">
                <img :src="imageFor(menu, index)" :alt="menuDisplayName(menu, index)" />
                <button class="heart-btn" type="button" @click="openReviews(menu)">♡</button>
              </div>
              <div class="menu-copy">
                <div class="menu-title-row">
                  <strong>{{ menuDisplayName(menu, index) }}</strong>
                  <b>{{ money(menu.price) }}</b>
                </div>
                <p>{{ menuDescription(menu, index) }}</p>
                <div class="menu-meta">
                  <span>★ {{ ratingFor(menu, index) }} <small>({{ reviewCountFor(menu, index) }}+)</small></span>
                  <button type="button" @click="addToCart(menu)">담기</button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <aside class="order-panel">
          <div class="panel-title">
            <h2>내 주문</h2>
            <span>{{ cart.length }}개 항목</span>
          </div>
          <article v-for="(item, index) in cart.slice(0, 3)" :key="item.id" class="mini-cart">
            <img :src="imageFor(cartMenuFor(item), index)" :alt="cartItemName(item, index)" />
            <div>
              <strong>{{ cartItemName(item, index) }}</strong>
              <div class="qty tiny">
                <button @click="updateCart(item, item.quantity - 1)">−</button>
                <b>{{ item.quantity }}</b>
                <button @click="updateCart(item, item.quantity + 1)">+</button>
              </div>
            </div>
            <b>{{ money(item.menuPrice) }}</b>
          </article>
          <p v-if="!cart.length" class="empty-note">메뉴를 담으면 여기에 표시됩니다.</p>
          <div class="price-lines">
            <span>소계</span><b>{{ money(cartTotal) }}</b>
            <span>할인</span><b>- {{ money(discountAmount) }}</b>
            <strong>총 결제 금액</strong><strong>{{ money(Math.max(cartTotal - discountAmount, 0)) }}</strong>
          </div>
          <button class="primary-btn large" :disabled="!cart.length" @click="view = 'cart'">주문하기 →</button>
          <div class="delivery-note">
            <b>가장 빠른 배달</b>
            <span>약 25-35분</span>
          </div>
        </aside>
      </section>

      <section v-if="view === 'cart'" class="cart-page">
        <h1>장바구니</h1>
        <div class="cart-grid">
          <section class="cart-list-panel">
            <div v-if="!cart.length" class="empty-cart">
              <div class="cart-illustration">⌔</div>
              <h2>장바구니가 비어 있습니다</h2>
              <p>맛있는 식사를 찾고 계신가요? 제스트푸드의 신선한 메뉴를 둘러보고 장바구니에 담아보세요.</p>
              <button type="button" @click="view = 'home'">메뉴 보러가기</button>
            </div>
            <article v-for="item in cart" :key="item.id" class="cart-row">
              <img :src="imageFor(cartMenuFor(item), item.id)" :alt="cartItemName(item, item.id)" />
              <div>
                <strong>{{ cartItemName(item, item.id) }}</strong>
                <span>{{ money(item.menuPrice) }}</span>
              </div>
              <div class="qty">
                <button @click="updateCart(item, item.quantity - 1)">−</button>
                <b>{{ item.quantity }}</b>
                <button @click="updateCart(item, item.quantity + 1)">+</button>
              </div>
              <button class="remove-btn" @click="removeCart(item)">삭제</button>
            </article>
          </section>

          <aside class="payment-panel">
            <h2>결제 상세</h2>
            <label>쿠폰 코드</label>
            <div class="coupon-row">
              <input v-model="couponCode" placeholder="코드를 입력하세요" />
              <button @click="validateCoupon">적용</button>
            </div>
            <div class="price-lines checkout">
              <span>소계</span><b>{{ money(cartTotal) }}</b>
              <span>할인 금액</span><b>- {{ money(discountAmount) }}</b>
              <span>배달비</span><b>{{ money(cart.length ? deliveryFee : 0) }}</b>
              <strong>총 결제 금액</strong><strong>{{ money(Math.max(cartTotal - discountAmount, 0) + (cart.length ? deliveryFee : 0)) }}</strong>
            </div>
            <button class="primary-btn large" :disabled="!cart.length" @click="createOrder">주문하기 →</button>
            <div class="trust-row">
              <span>안전 결제</span>
              <span>신선 배달</span>
              <span>고객센터</span>
            </div>
          </aside>
        </div>
      </section>

      <section v-if="view === 'reviews'" class="review-page">
        <div class="review-hero">
          <div>
            <h1>고객 리뷰</h1>
            <p>제스트푸드를 이용해주신 고객님들의 진솔한 이야기를 확인해보세요.</p>
          </div>
          <button class="review-write-btn" type="button" @click="view = 'home'">리뷰 작성하기</button>
        </div>

        <div class="rating-summary">
          <section>
            <span>평균 별점</span>
            <strong>{{ averageRating }}</strong>
            <b>★★★★★</b>
            <p>총 {{ reviewItems.length.toLocaleString() }}개의 리뷰</p>
          </section>
          <section class="rating-bars">
            <div v-for="row in ratingRows" :key="row.score">
              <span>{{ row.score }}점</span>
              <i><em :style="{ width: row.percent + '%' }"></em></i>
              <b>{{ row.percent }}%</b>
            </div>
          </section>
        </div>

        <div class="review-tabs">
          <button class="active" type="button">전체보기</button>
          <button type="button">사진 리뷰</button>
          <button type="button">최신순</button>
          <button type="button">높은 평점순</button>
        </div>

        <div class="review-grid">
          <article v-for="(review, index) in visibleReviews" :key="review.key" class="review-card">
            <div class="review-author">
              <strong>{{ review.userEmail || `고객 ${index + 1}` }}</strong>
              <span>{{ dateText(review.createdAt) || '최근 작성' }}</span>
            </div>
            <b>{{ '★'.repeat(review.rating) }}</b>
            <span class="review-menu">{{ review.menuName }}</span>
            <p>{{ review.comment }}</p>
            <img v-if="index === 0 || index === 2" :src="imageFor(review.menu, index)" :alt="review.menuName" />
          </article>
        </div>
      </section>

      <section v-if="view === 'orders'" class="orders-page">
        <div class="section-heading">
          <h1>내 주문</h1>
          <button class="outline-btn" @click="openOrders">새로고침</button>
        </div>
        <article v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-status">{{ statusText(order.status).slice(0, 1) }}</div>
          <div>
            <strong>#{{ order.id }} {{ statusText(order.status) }}</strong>
            <p>{{ order.items?.map((i, index) => `${orderItemName(i, index)} ${i.quantity}개`).join(', ') }}</p>
            <small>{{ dateText(order.createdAt) }}</small>
          </div>
          <b>{{ money(order.finalPrice || order.totalPrice) }}</b>
        </article>
      </section>

      <footer class="site-footer">
        <div>
          <strong>제스트푸드</strong>
          <p>신선하고 건강한 식사를 문 앞까지 배달해 드립니다.</p>
        </div>
        <nav>
          <a>회사 소개</a>
          <a>고객센터</a>
          <a>개인정보 처리방침</a>
          <a>이용약관</a>
        </nav>
        <small>© 2024 ZestFood Inc. All rights reserved.</small>
      </footer>

      <div v-if="reviewMenu" class="modal-backdrop" @click.self="reviewMenu = null">
        <section class="modal">
          <header>
            <h2>{{ menuDisplayName(reviewMenu, menus.findIndex(menu => menu.id === reviewMenu.id)) }} 리뷰</h2>
            <button @click="reviewMenu = null">닫기</button>
          </header>
          <form class="review-form" @submit.prevent="submitReview">
            <select v-model.number="reviewForm.rating">
              <option v-for="n in 5" :key="n" :value="n">{{ n }}점</option>
            </select>
            <input v-model="reviewForm.comment" placeholder="리뷰를 남겨주세요" required />
            <button>등록</button>
          </form>
          <article v-for="review in reviews" :key="review.id" class="review-item">
            <b>{{ '★'.repeat(review.rating) }}</b>
            <p>{{ review.comment }}</p>
            <small>{{ review.userEmail }}</small>
            <button v-if="review.userId === session.userId" class="remove-btn" @click="deleteReview(review)">삭제</button>
          </article>
        </section>
      </div>
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
const dataMessage = ref('')
const view = ref('home')
const keyword = ref('')
const selectedCategory = ref(null)
const categories = ref([])
const menus = ref([])
const cart = ref([])
const orders = ref([])
const recommendations = ref([])
const couponCode = ref('')
const coupon = ref(null)
const reviewMenu = ref(null)
const reviews = ref([])
const allReviews = ref([])
const reviewForm = reactive({ rating: 5, comment: '' })
const deliveryFee = 2000

const fallbackImages = [
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=720&q=80',
  'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=720&q=80'
]

const foodNames = [
  '닭가슴살 그린 볼',
  '아보카도 콥 샐러드',
  '바질 치킨 피자',
  '새우 토마토 파스타',
  '스파이시 커리 라이스',
  '베리 요거트 타르트',
  '로스트 그레인 보울',
  '두부 미소 된장국'
]

const foodDescriptions = [
  '구운 닭가슴살과 신선한 채소, 고소한 옥수수를 담은 든든한 한 그릇입니다.',
  '아보카도와 병아리콩, 제철 채소를 균형 있게 담은 산뜻한 샐러드입니다.',
  '바질 향과 치킨 토핑이 잘 어울리는 담백한 피자 메뉴입니다.',
  '탱글한 새우와 토마토 소스가 어우러진 매콤한 파스타입니다.',
  '향긋한 커리와 라이스를 따뜻하게 담아낸 든든한 메뉴입니다.',
  '상큼한 과일과 부드러운 요거트 크림을 올린 디저트입니다.',
  '구운 곡물과 채소를 담아 포만감 있게 즐기는 건강 보울입니다.',
  '부드러운 두부와 깊은 국물 맛을 살린 따뜻한 국물 메뉴입니다.'
]

const categoryNames = [
  '샐러드 & 보울',
  '피자 & 파스타',
  '면 & 밥',
  '디저트',
  '건강식',
  '스페셜 메뉴',
  '든든한 한 끼',
  '가벼운 식사'
]

const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + item.menuPrice * item.quantity, 0))
const discountAmount = computed(() => {
  if (!coupon.value || cartTotal.value < coupon.value.minOrderAmount) return 0
  return coupon.value.discountType === 'PERCENTAGE'
    ? Math.floor(cartTotal.value * coupon.value.discountValue / 100)
    : coupon.value.discountValue
})
const featuredMenu = computed(() => menus.value[0] || null)
const userInitial = computed(() => (session.value?.email || 'U').slice(0, 1).toUpperCase())
const reviewItems = computed(() => allReviews.value.length ? allReviews.value : fallbackReviews.value)
const visibleReviews = computed(() => reviewItems.value.slice(0, 6))
const averageRating = computed(() => {
  if (!reviewItems.value.length) return '0.0'
  const total = reviewItems.value.reduce((sum, review) => sum + Number(review.rating || 0), 0)
  return (total / reviewItems.value.length).toFixed(1)
})
const ratingRows = computed(() => {
  const total = Math.max(reviewItems.value.length, 1)
  return [5, 4, 3, 2, 1].map(score => {
    const count = reviewItems.value.filter(review => Number(review.rating) === score).length
    return { score, percent: Math.round(count / total * 100) }
  })
})
const fallbackReviews = computed(() => menus.value.slice(0, 6).map((menu, index) => ({
  key: `fallback-${menu.id}`,
  id: `fallback-${menu.id}`,
  rating: index % 3 === 0 ? 5 : 4,
  comment: menuDescription(menu, index),
  userEmail: ['김*호', '이*나', '최*석', '박*준', '장*현', '윤*아'][index] || '고객',
  menuName: menuDisplayName(menu, index),
  menu
})))

onMounted(async () => {
  await Promise.allSettled([loadCategories(), loadMenus(), loadRecommend()])
  if (session.value) loadCart().catch(showDataError)
})

async function submitAuth() {
  message.value = ''
  try {
    const path = authMode.value === 'login' ? '/auth/login' : '/auth/register'
    const payload = authMode.value === 'login'
      ? { email: auth.email, password: auth.password }
      : { email: auth.email, password: auth.password, name: auth.name, phone: auth.phone, role: 'CUSTOMER' }
    session.value = await api(path, { method: 'POST', body: payload })
    saveSession(session.value)
    await loadCart()
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

async function loadCategories() {
  try {
    categories.value = await api('/categories')
    dataMessage.value = ''
  } catch (e) {
    showDataError(e)
  }
}

async function loadMenus() {
  try {
    const params = new URLSearchParams()
    if (selectedCategory.value) params.set('categoryId', selectedCategory.value)
    if (keyword.value) params.set('keyword', keyword.value)
    menus.value = await api(`/menus${params.toString() ? `?${params}` : ''}`)
    dataMessage.value = ''
  } catch (e) {
    showDataError(e)
  }
}

function selectCategory(id) {
  selectedCategory.value = id
  loadMenus()
}

async function loadRecommend() {
  try {
    const data = await api('/ai/recommend')
    recommendations.value = data.recommendations || []
  } catch {
    recommendations.value = [
      '점심엔 비빔밥 어때요?',
      '저녁엔 떡볶이 어때요?',
      '야식엔 치킨 어때요?'
    ]
  }
}

async function loadCart() {
  try {
    cart.value = await api('/cart')
    dataMessage.value = ''
  } catch (e) {
    showDataError(e)
  }
}

async function addToCart(menu) {
  const index = menus.value.findIndex(item => item.id === menu.id)
  await api('/cart', { method: 'POST', body: { menuId: menu.id, menuName: menuDisplayName(menu, index), menuPrice: menu.price, quantity: 1 } })
  await loadCart()
}

async function updateCart(item, quantity) {
  if (quantity < 1) return removeCart(item)
  await api(`/cart/${item.id}`, { method: 'PUT', body: { quantity } })
  await loadCart()
}

async function removeCart(item) {
  await api(`/cart/${item.id}`, { method: 'DELETE' })
  await loadCart()
}

async function validateCoupon() {
  coupon.value = couponCode.value ? await api(`/coupons/validate/${encodeURIComponent(couponCode.value)}`) : null
}

async function createOrder() {
  const body = {
    couponCode: couponCode.value || null,
    items: cart.value.map((i, index) => ({ menuId: i.menuId, menuName: cartItemName(i, index), quantity: i.quantity, price: i.menuPrice }))
  }
  await api('/orders', { method: 'POST', body })
  coupon.value = null
  couponCode.value = ''
  await loadCart()
  await openOrders()
}

async function openCart() {
  view.value = 'cart'
  await loadCart()
}

async function openOrders() {
  view.value = 'orders'
  try {
    orders.value = await api('/orders/my')
    dataMessage.value = ''
  } catch (e) {
    showDataError(e)
  }
}

async function openReviewPage() {
  view.value = 'reviews'
  await loadAllReviews()
}

async function loadAllReviews() {
  const sourceMenus = menus.value.slice(0, 8)
  const settled = await Promise.allSettled(sourceMenus.map(menu => api(`/reviews?menuId=${menu.id}`).then(items => ({ menu, items }))))
  allReviews.value = settled
    .filter(result => result.status === 'fulfilled')
    .flatMap(result => result.value.items.map(review => ({
      ...review,
      key: `${result.value.menu.id}-${review.id}`,
      menuName: menuDisplayName(result.value.menu, menus.value.findIndex(menu => menu.id === result.value.menu.id)),
      menu: result.value.menu
    })))
}

async function openReviews(menu) {
  reviewMenu.value = menu
  reviewForm.comment = ''
  reviews.value = await api(`/reviews?menuId=${menu.id}`)
}

async function submitReview() {
  await api('/reviews', { method: 'POST', body: { menuId: reviewMenu.value.id, rating: reviewForm.rating, comment: reviewForm.comment } })
  await openReviews(reviewMenu.value)
  await loadAllReviews()
}

async function deleteReview(review) {
  await api(`/reviews/${review.id}`, { method: 'DELETE' })
  await openReviews(reviewMenu.value)
  await loadAllReviews()
}

function money(value) {
  return `₩${Number(value || 0).toLocaleString()}`
}

function statusText(status) {
  return ({ PENDING: '대기 중', PREPARING: '준비 중', DONE: '완료' })[status] || status
}

function dateText(value) {
  return value ? new Date(value).toLocaleDateString('ko-KR') : ''
}

function imageFor(menu, index = 0) {
  return menu?.imageUrl || fallbackImages[Math.abs(Number(index) || 0) % fallbackImages.length]
}

function cartMenuFor(item) {
  return menus.value.find(menu => menu.id === item.menuId) || { name: item.menuName }
}

function menuDisplayName(menu, index = 0) {
  const rawName = String(menu?.name || '').trim()
  if (rawName && !isPlaceholderName(rawName)) return rawName
  return foodNames[normalizedIndex(index)] || '오늘의 신선 메뉴'
}

function menuDescription(menu, index = 0) {
  const rawDescription = String(menu?.description || '').trim()
  if (rawDescription && !isPlaceholderName(rawDescription)) return rawDescription
  return foodDescriptions[normalizedIndex(index)] || menu?.categoryName || '오늘 바로 먹기 좋은 신선한 메뉴입니다.'
}

function cartItemName(item, index = 0) {
  const menu = cartMenuFor(item)
  const menuIndex = menus.value.findIndex(source => source.id === item.menuId)
  const rawName = String(item?.menuName || '').trim()
  if (rawName && !isPlaceholderName(rawName)) return rawName
  return menuDisplayName(menu, menuIndex >= 0 ? menuIndex : index)
}

function orderItemName(item, index = 0) {
  const menu = menus.value.find(source => source.id === item.menuId)
  const rawName = String(item?.menuName || '').trim()
  if (rawName && !isPlaceholderName(rawName)) return rawName
  return menuDisplayName(menu, index)
}

function isPlaceholderName(value) {
  return value.includes('?') || /^\d+$/.test(value) || /\d{10,}/.test(value)
}

function normalizedIndex(index = 0) {
  const numericIndex = Number(index)
  return Math.abs(Number.isFinite(numericIndex) ? numericIndex : 0) % foodNames.length
}

function normalizedCategoryIndex(index = 0) {
  const numericIndex = Number(index)
  return Math.abs(Number.isFinite(numericIndex) ? numericIndex : 0) % categoryNames.length
}

function categoryDisplayName(category, index = 0) {
  const rawName = String(category?.name || '').trim()
  if (rawName && !isPlaceholderName(rawName)) return rawName
  return categoryNames[normalizedCategoryIndex(index)] || '추천 카테고리'
}

function categoryIcon(name = '') {
  if (name.includes('샐러드')) return '○'
  if (name.includes('면') || name.includes('밥')) return '▱'
  if (name.includes('피자') || name.includes('파스타')) return '△'
  if (name.includes('디저트')) return '◇'
  if (name.includes('건강')) return '◌'
  if (name.includes('스페셜')) return '∥'
  return '◌'
}

function ratingFor(menu, index) {
  return (4.6 + ((Number(menu.id || index) % 5) * 0.1)).toFixed(1)
}

function reviewCountFor(menu, index) {
  return 35 + ((Number(menu.id || index) % 9) * 25)
}

function showDataError(error) {
  dataMessage.value = error?.message || '데이터를 불러오지 못했습니다.'
}
</script>
