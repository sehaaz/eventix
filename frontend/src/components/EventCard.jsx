import { Link } from 'react-router-dom';
import { formatDate, formatPrice } from '../format';

export default function EventCard({ event }) {
  const remaining = event.totalQuota - event.soldCount;

  return (
    <Link
      to={`/events/${event.id}`}
      className="block overflow-hidden rounded-xl border border-stone-200 bg-white hover:border-teal-300"
    >
      {event.imageUrl ? (
        <img src={event.imageUrl} alt={event.title} className="h-40 w-full object-cover" />
      ) : (
        <div className="h-40 w-full bg-teal-50" />
      )}
      <div className="space-y-1 p-4">
        <h3 className="font-medium text-stone-900">{event.title}</h3>
        <p className="text-sm text-stone-500">
          {event.city} · {formatDate(event.eventDate)}
        </p>
        <div className="flex items-center justify-between pt-2 text-sm">
          <span className="font-medium text-teal-700">{formatPrice(event.price)}</span>
          <span className="text-stone-400">
            {remaining > 0 ? `${remaining} bilet kaldı` : 'Tükendi'}
          </span>
        </div>
      </div>
    </Link>
  );
}
