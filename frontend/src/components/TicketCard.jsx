import { useEffect, useState } from 'react';
import client, { errorMessage } from '../api/client';
import { formatDate } from '../format';

// QR ve PDF JWT ile korunduğu için <img src> / <a href> yerine axios ile blob olarak çekilir.
export default function TicketCard({ ticket, event }) {
  const [qrUrl, setQrUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let url;
    let cancelled = false;
    client
      .get(`/api/tickets/${ticket.ticketCode}/qr`, { responseType: 'blob' })
      .then((res) => {
        if (cancelled) return;
        url = URL.createObjectURL(res.data);
        setQrUrl(url);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [ticket.ticketCode]);

  async function downloadPdf() {
    setError(null);
    try {
      const res = await client.get(`/api/tickets/${ticket.ticketCode}/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${ticket.ticketCode}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="flex gap-4 rounded-xl border border-stone-200 bg-white p-4">
      <div className="h-28 w-28 shrink-0 rounded-md bg-stone-100">
        {qrUrl && <img src={qrUrl} alt="QR kod" className="h-28 w-28 rounded-md" />}
      </div>
      <div className="flex min-w-0 flex-1 flex-col justify-between">
        <div className="space-y-1">
          <h3 className="font-medium text-stone-900">{event?.title ?? `Etkinlik #${ticket.eventId}`}</h3>
          {event && (
            <p className="text-sm text-stone-500">
              {event.city} · {formatDate(event.eventDate)}
            </p>
          )}
          <p className="truncate font-mono text-xs text-stone-400">{ticket.ticketCode}</p>
        </div>
        <div className="flex items-center justify-between pt-2">
          <span className={`text-xs ${ticket.used ? 'text-stone-400' : 'text-emerald-600'}`}>
            {ticket.used ? 'Kullanıldı' : 'Geçerli'}
          </span>
          <button
            onClick={downloadPdf}
            className="rounded-md border border-stone-300 px-3 py-1 text-sm hover:bg-stone-100"
          >
            PDF indir
          </button>
        </div>
        {error && <p className="pt-1 text-xs text-rose-600">{error}</p>}
      </div>
    </div>
  );
}
