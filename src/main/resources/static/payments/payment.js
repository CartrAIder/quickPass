const API_BASE_URL = window.location.origin;
const message = document.querySelector('#message');
const payButton = document.querySelector('#pay-button');
const orderId = new URLSearchParams(window.location.search).get('orderId');
// 실제 앱에서는 로그인 직후 메모리 상태 저장소에 넣은 액세스 토큰을 전달한다.
const accessToken = sessionStorage.getItem('accessToken');

function showMessage(value) {
  message.textContent = value;
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      ...options.headers
    },
    credentials: 'include'
  });
  const body = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message ?? `요청에 실패했습니다. (${response.status})`);
  return body;
}

if (!orderId || !accessToken) {
  showMessage('주문 ID 또는 로그인 정보가 없습니다. 주문 화면에서 다시 시도해 주세요.');
} else {
  document.querySelector('#order-summary').textContent = `${orderId} 주문을 결제합니다.`;
  payButton.disabled = false;
  payButton.addEventListener('click', async () => {
    payButton.disabled = true;
    try {
      // 버튼을 누른 뒤에만 READY 상태의 결제 시도를 만든다.
      const attempt = await api(
        `/api/orders/${encodeURIComponent(orderId)}/payment-attempts`,
        { method: 'POST', body: '{}' }
      );
      const config = await api('/api/payments/client-key');
      if (!config.clientKey) throw new Error('토스 테스트 클라이언트 키가 설정되지 않았습니다.');

      document.querySelector('#order-summary').textContent =
        `${attempt.orderName} · ${attempt.amount.toLocaleString()}원`;
      // 토스 successUrl에는 paymentKey/orderId/amount가 붙는다. attempt ID는 브라우저 저장소로 이어 준다.
      sessionStorage.setItem(`payment-attempt:${attempt.orderId}`, attempt.paymentAttemptId);
      const tossPayments = TossPayments(config.clientKey);
      const payment = tossPayments.payment({ customerKey: `quickpass-${attempt.orderId}` });
      await payment.requestPayment({
        method: 'CARD',
        amount: { currency: 'KRW', value: attempt.amount },
        orderId: attempt.orderId,
        // 주문 생성 시 서버가 확정해 저장한 이름을 그대로 사용한다.
        orderName: attempt.orderName,
        successUrl: `${window.location.origin}/payments/success.html`,
        failUrl: `${window.location.origin}/payments/fail.html`
      });
    } catch (error) {
      showMessage(error.message ?? '결제창을 열지 못했습니다. 네트워크 상태를 확인해 주세요.');
      payButton.disabled = false;
    }
  });
}
