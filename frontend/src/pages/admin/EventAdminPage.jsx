import { useCallback, useEffect, useState } from 'react';
import client, { errorMessage } from '../../api/client';
import { formatDate, formatPrice } from '../../format';

const PAGE_SIZE = 20;

const EMPTY_FORM = {
  title: '',
  description: '',
  venue: '',
  city: '',
  eventDate: '',
  price: '',
  totalQuota: '',
  imageUrl: '',
};

// Instant -> <input type="datetime-local"> değeri (yerel saat, "YYYY-MM-DDTHH:mm").
function toLocalInput(iso) {
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

const inputClass =
  'w-full rounded-md border border-stone-300 px-3 py-2 focus:border-teal-500 focus:outline-none';

export default function EventAdminPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    client
      .get('/api/events', { params: { page, size: PAGE_SIZE } })
      .then((res) => setData(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [page]);

  useEffect(load, [load]);

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value });
  }

  function startEdit(event) {
    setError(null);
    setEditingId(event.id);
    setForm({
      title: event.title,
      description: event.description ?? '',
      venue: event.venue,
      city: event.city,
      eventDate: toLocalInput(event.eventDate),
      price: String(event.price),
      totalQuota: String(event.totalQuota),
      imageUrl: event.imageUrl ?? '',
    });
  }

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    const body = {
      ...form,
      eventDate: new Date(form.eventDate).toISOString(),
      price: Number(form.price),
      totalQuota: Number(form.totalQuota),
      imageUrl: form.imageUrl || null,
    };
    try {
      if (editingId) {
        await client.put(`/api/events/${editingId}`, body);
      } else {
        await client.post('/api/events', body);
      }
      resetForm();
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(event) {
    if (!window.confirm(`"${event.title}" silinsin mi?`)) return;
    setError(null);
    try {
      await client.delete(`/api/events/${event.id}`);
      if (editingId === event.id) resetForm();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-5">
      <form
        onSubmit={handleSubmit}
        className="space-y-3 self-start rounded-xl border border-stone-200 bg-white p-5 lg:col-span-2"
      >
        <h2 className="font-semibold text-stone-900">
          {editingId ? `Etkinlik #${editingId} düzenle` : 'Yeni etkinlik'}
        </h2>
        <input required placeholder="Başlık" value={form.title} onChange={update('title')} className={inputClass} />
        <textarea
          rows={3}
          placeholder="Açıklama"
          value={form.description}
          onChange={update('description')}
          className={inputClass}
        />
        <div className="grid grid-cols-2 gap-3">
          <input required placeholder="Mekan" value={form.venue} onChange={update('venue')} className={inputClass} />
          <input required placeholder="Şehir" value={form.city} onChange={update('city')} className={inputClass} />
        </div>
        <input
          type="datetime-local"
          required
          value={form.eventDate}
          onChange={update('eventDate')}
          className={inputClass}
        />
        <div className="grid grid-cols-2 gap-3">
          <input
            type="number"
            required
            min="0"
            step="0.01"
            placeholder="Fiyat"
            value={form.price}
            onChange={update('price')}
            className={inputClass}
          />
          <input
            type="number"
            required
            min="1"
            placeholder="Kontenjan"
            value={form.totalQuota}
            onChange={update('totalQuota')}
            className={inputClass}
          />
        </div>
        <input placeholder="Görsel URL" value={form.imageUrl} onChange={update('imageUrl')} className={inputClass} />
        {error && <p className="text-sm text-rose-600">{error}</p>}
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 rounded-md bg-teal-600 py-2 text-white hover:bg-teal-700 disabled:opacity-50"
          >
            {editingId ? 'Kaydet' : 'Ekle'}
          </button>
          {editingId && (
            <button
              type="button"
              onClick={resetForm}
              className="rounded-md border border-stone-300 px-4 py-2 hover:bg-stone-100"
            >
              Vazgeç
            </button>
          )}
        </div>
      </form>

      <div className="space-y-4 lg:col-span-3">
        <div className="divide-y divide-stone-100 rounded-xl border border-stone-200 bg-white">
          {data?.content.map((event) => (
            <div key={event.id} className="flex items-center justify-between gap-4 p-4">
              <div className="min-w-0">
                <p className="truncate font-medium text-stone-900">{event.title}</p>
                <p className="text-sm text-stone-500">
                  {event.city} · {formatDate(event.eventDate)} · {formatPrice(event.price)} ·{' '}
                  {event.soldCount}/{event.totalQuota}
                </p>
              </div>
              <div className="flex shrink-0 gap-3 text-sm">
                <button onClick={() => startEdit(event)} className="text-teal-700 hover:underline">
                  Düzenle
                </button>
                <button onClick={() => handleDelete(event)} className="text-rose-600 hover:underline">
                  Sil
                </button>
              </div>
            </div>
          ))}
          {data?.content.length === 0 && <p className="p-6 text-center text-stone-400">Etkinlik yok.</p>}
        </div>

        {data && data.page.totalPages > 1 && (
          <div className="flex items-center justify-center gap-4 text-sm">
            <button
              onClick={() => setPage(page - 1)}
              disabled={page === 0}
              className="rounded-md border border-stone-300 px-3 py-1.5 hover:bg-stone-100 disabled:opacity-40"
            >
              Önceki
            </button>
            <span className="text-stone-500">
              {page + 1} / {data.page.totalPages}
            </span>
            <button
              onClick={() => setPage(page + 1)}
              disabled={page + 1 >= data.page.totalPages}
              className="rounded-md border border-stone-300 px-3 py-1.5 hover:bg-stone-100 disabled:opacity-40"
            >
              Sonraki
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
