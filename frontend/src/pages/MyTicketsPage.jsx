import { useEffect, useState } from 'react';
import client, { errorMessage } from '../api/client';
import TicketCard from '../components/TicketCard';

export default function MyTicketsPage() {
  const [tickets, setTickets] = useState(null);
  const [events, setEvents] = useState({});
  const [error, setError] = useState(null);

  useEffect(() => {
    client
      .get('/api/tickets/me')
      .then(async (res) => {
        setTickets(res.data);
        // Bilet yanıtında etkinlik bilgisi yok; başlık/tarih için benzersiz etkinlikler çekilir.
        const ids = [...new Set(res.data.map((t) => t.eventId))];
        const responses = await Promise.all(
          ids.map((id) => client.get(`/api/events/${id}`).catch(() => null)),
        );
        const byId = {};
        responses.forEach((r) => r && (byId[r.data.id] = r.data));
        setEvents(byId);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-stone-900">Biletlerim</h1>
      {error && <p className="text-sm text-rose-600">{error}</p>}
      {tickets && tickets.length === 0 && (
        <p className="py-12 text-center text-stone-400">Henüz biletin yok.</p>
      )}
      <div className="grid gap-4 md:grid-cols-2">
        {tickets?.map((ticket) => (
          <TicketCard key={ticket.ticketCode} ticket={ticket} event={events[ticket.eventId]} />
        ))}
      </div>
    </div>
  );
}
