import client from './client';

const yq = (yearId) => (yearId ? { params: { yearId } } : {});

export const api = {
  // ---- Auth ----
  login: (accountNo, password) => client.post('/auth/login', { accountNo, password }),
  changePassword: (currentPassword, newPassword) =>
    client.post('/auth/change-password', { currentPassword, newPassword }),
  forgotPassword: (accountNo) => client.post('/auth/forgot-password', { accountNo }),
  resetPassword: (token, newPassword) => client.post('/auth/reset-password', { token, newPassword }),

  // ---- Financial years ----
  listYears: () => client.get('/financial-years'),
  currentYear: () => client.get('/financial-years/current'),
  createYear: (startYear) => client.post('/financial-years', { startYear }),

  // ---- Members ----
  listMembers: () => client.get('/members'),
  getMember: (id) => client.get(`/members/${id}`),
  createMember: (body) => client.post('/members', body),
  updateMember: (id, body) => client.put(`/members/${id}`, body),
  deleteMember: (id) => client.delete(`/members/${id}`),
  resetMemberPassword: (id) => client.post(`/members/${id}/reset-password`),

  // ---- Ledgers (shares / compulsory / other) ----
  ledger: (memberId, section, yearId) => client.get(`/members/${memberId}/${section}`, yq(yearId)),
  addLedger: (memberId, section, body, yearId) =>
    client.post(`/members/${memberId}/${section}`, body, yq(yearId)),
  updateLedger: (memberId, section, entryId, body) =>
    client.put(`/members/${memberId}/${section}/${entryId}`, body),
  deleteLedger: (memberId, section, entryId) =>
    client.delete(`/members/${memberId}/${section}/${entryId}`),

  // ---- Loans ----
  listLoans: (memberId) => client.get(`/members/${memberId}/loans`),
  loanDetail: (memberId, loanId) => client.get(`/members/${memberId}/loans/${loanId}`),
  disburseLoan: (memberId, body, yearId) =>
    client.post(`/members/${memberId}/loans`, body, yq(yearId)),
  postInterest: (memberId, loanId, body, yearId) =>
    client.post(`/members/${memberId}/loans/${loanId}/interest`, body, yq(yearId)),
  repay: (memberId, loanId, body, yearId) =>
    client.post(`/members/${memberId}/loans/${loanId}/repayments`, body, yq(yearId)),
  deleteLoanTxn: (memberId, loanId, txnId) =>
    client.delete(`/members/${memberId}/loans/${loanId}/txns/${txnId}`),
  closeLoan: (memberId, loanId) => client.post(`/members/${memberId}/loans/${loanId}/close`),

  // ---- Favour stood ----
  favourStood: (memberId, yearId) => client.get(`/members/${memberId}/favour-stood`, yq(yearId)),
  addFavourStood: (memberId, body, yearId) =>
    client.post(`/members/${memberId}/favour-stood`, body, yq(yearId)),
  deleteFavourStood: (memberId, entryId) =>
    client.delete(`/members/${memberId}/favour-stood/${entryId}`),

  // ---- Year-end ----
  yearEndPreview: (yearId) => client.get(`/financial-years/${yearId}/year-end/preview`),
  closeYear: (yearId, splits) => client.post(`/financial-years/${yearId}/close`, { splits }),

  // ---- Export (returns blob) ----
  exportMember: (memberId, yearId) =>
    client.get(`/members/${memberId}/export`, { params: { yearId }, responseType: 'blob' }),
  exportAll: (yearId) =>
    client.get('/export/all', { params: { yearId }, responseType: 'blob' }),
};

/** Triggers a browser download for an xlsx blob response. */
export function downloadBlob(response, fallbackName = 'export.xlsx') {
  const disposition = response.headers['content-disposition'] || '';
  const match = /filename="?([^"]+)"?/.exec(disposition);
  const name = match ? match[1] : fallbackName;
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
