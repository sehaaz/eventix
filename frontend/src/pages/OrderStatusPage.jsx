import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import SagaStepper from '../components/SagaStepper';
import { formatPrice } from '../format';

const POLL_INTERVAL_MS = 2000;
const REDIRECT_DELAY_MS = 1500;

export default function OrderStatusPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    let timer;

    // Bir istek bitmeden yenisi başlamasın diye setInterval yerine zincirli setTimeout.
    async function poll() {
      try {
        const { data } = await client.get(`/api/orders/${id}`);
        if (cancelled) return;
        setOrder(data);
        if (data.status === 'COMPLETED') {
          timer = setTimeout(() => navigate('/tickets'), REDIRECT_DELAY_MS);
          return;
        }
        if (data.status === 'FAILED') return;
        timer = setTimeout(poll, POLL_INTERVAL_MS);
      } catch (err) {
        if (!cancelled) setError(errorMessage(err));
      }
    }

    poll();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [id, navigate]);

  return (
    <div className="mx-auto max-w-md rounded-xl border border-stone-200 bg-white p-6">
      <h1 className="mb-1 text-xl font-semibold text-stone-900">Sipariş #{id}</h1>
      {order && (
        <p className="mb-6 text-sm text-stone-500">
          {order.eventTitle} · {order.quantity} adet · {formatPrice(order.totalPrice)}
        </p>
      )}
      {order && <SagaStepper status={order.status} failureReason={order.failureReason} />}
      {error && <p className="text-sm text-rose-600">{error}</p>}
    </div>
  );
}
