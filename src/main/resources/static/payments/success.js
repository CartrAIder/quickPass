const params = new URLSearchParams(window.location.search);
const message = document.querySelector('#message');
const paymentKey = params.get('paymentKey');
const orderId = params.get('orderId');
const amount = Number(params.get('amount'));
const paymentAttemptId = orderId && sessionStorage.getItem(`payment-attempt:${orderId}`);
const accessToken = sessionStorage.getItem('accessToken');

async function confirm() {
  if (!paymentKey || !orderId || !Number.isSafeInteger(amount) || !paymentAttemptId || !accessToken) {
    throw new Error('결제 승인에 필요한 정보가 없습니다. 주문 목록에서 결제 상태를 확인해 주세요.');
  }
  const response = await fetch('/api/payments/confirm', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ paymentKey, orderId, amount, paymentAttemptId })
  });
  const result = await response.json().catch(() => null);
  if (!response.ok || result?.status !== 'APPROVED') {
    throw new Error(result?.message ?? '결제 승인에 실패했습니다. 결제 내역을 확인해 주세요.');
  }
  sessionStorage.removeItem(`payment-attempt:${orderId}`);
  message.textContent = '결제가 완료되었습니다.';
}

confirm().catch((error) => { message.textContent = error.message; });
