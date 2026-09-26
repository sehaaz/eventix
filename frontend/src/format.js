export function formatDate(iso) {
  return new Date(iso).toLocaleString('tr-TR', { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatPrice(value) {
  return Number(value).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' });
}
