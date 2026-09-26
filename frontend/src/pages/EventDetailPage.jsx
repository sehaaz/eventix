import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { formatDate, formatPrice } from '../format';

const MAX_PER_ORDER = 10;

export default function EventDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [event, setEvent] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    client
      .get(`/api/events/${id}`)
      .then((res) => setEvent(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [id]);

  async function handleBuy() {
    if (!user) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const { data } = await client.post('/api/orders', { eventId: event.id, quantity });
      navigate(`/orders/${data.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  }

  if (!event) {
    return error ? <p className="text-sm text-rose-600">{error}</p> : null;
  }

  const remaining = event.totalQuota - event.soldCount;
  const maxQuantity = Math.min(remaining, MAX_PER_ORDER);

  return (
    <div className="overflow-hidden rounded-xl border border-stone-200 bg-white">
      {event.imageUrl ? (
        <img src={event.imageUrl} alt={event.title} className="h-64 w-full object-cover" />
      ) : (
        <div className="h-64 w-full bg-teal-50" />
      )}
      <div className="grid gap-8 p-6 md:grid-cols-3">
        <div className="space-y-3 md:col-span-2">
          <h1 className="text-2xl font-semibold text-stone-900">{event.title}</h1>
          <p className="text-stone-500">
            {event.venue}, {event.city} · {formatDate(event.eventDate)}
          </p>
          {event.description && (
            <p className="whitespace-pre-line leading-relaxed text-stone-700">{event.description}</p>
          )}
        </div>

        <div className="space-y-4 rounded-lg bg-stone-50 p-4">
          <div className="flex items-baseline justify-between">
            <span className="text-xl font-semibold text-teal-700">{formatPrice(event.price)}</span>
            <span className="text-sm text-stone-400">
              {remaining > 0 ? `${remaining} bilet kaldı` : 'Tükendi'}
            </span>
          </div>

          {remaining > 0 && (
            <>
              <label className="flex items-center justify-between text-sm text-stone-600">
                Adet
                <select
                  value={quantity}
                  onChange={(e) => setQuantity(Number(e.target.value))}
                  className="rounded-md border border-stone-300 bg-white px-3 py-1.5 focus:border-teal-500 focus:outline-none"
                >
                  {Array.from({ length: maxQuantity }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </label>
              <div className="flex justify-between border-t border-stone-200 pt-3 text-sm">
                <span className="text-stone-500">Toplam</span>
                <span className="font-medium">{formatPrice(event.price * quantity)}</span>
              </div>
              <button
                onClick={handleBuy}
                disabled={submitting}
                className="w-full rounded-md bg-teal-600 py-2 text-white hover:bg-teal-700 disabled:opacity-50"
              >
                Satın Al
              </button>
            </>
          )}

          {error && <p className="text-sm text-rose-600">{error}</p>}
        </div>
      </div>
    </div>
  );
}
