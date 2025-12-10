import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { Provider } from 'react-redux';
import { store } from '../../src/store/store';
import IssueVCNForm from '../../src/components/IssueVCNForm';
import { server } from '../setupTests';
import { http, HttpResponse } from 'msw';

describe('IssueVCNForm', () => {
  beforeEach(() => {
    server.use(
      http.post('*/cards/vcn', () => {
        return HttpResponse.json({
          tokenId: 'VCN#new',
          realCardId: 'REAL#acc123',
          vcn: '4532 9876 5432 1098',
          spendLimit: 750,
          expiresAt: new Date().toISOString(),
          status: 'ACTIVE',
        });
      })
    );
  });

  it('issues VCN and shows success', async () => {
    render(
      <Provider store={store}>
        <IssueVCNForm realCardId="REAL#acc123" />
      </Provider>
    );

    await userEvent.type(screen.getByPlaceholderText(/500.00/i), '750');
    await userEvent.click(screen.getByText('Issue VCN'));

    await waitFor(() => {
      expect(screen.getByText(/VCN issued successfully/i)).toBeInTheDocument();
    });
  });

  it('shows error on failure', async () => {
    server.use(
      http.post('*/cards/vcn', () => {
        return new HttpResponse(null, { status: 400 });
      })
    );

    render(
      <Provider store={store}>
        <IssueVCNForm realCardId="REAL#acc123" />
      </Provider>
    );

    await userEvent.type(screen.getByPlaceholderText(/500.00/i), '99999');
    await userEvent.click(screen.getByText('Issue VCN'));

    await waitFor(() => {
      expect(screen.getByText(/Failed to issue VCN/i)).toBeInTheDocument();
    });
  });
});