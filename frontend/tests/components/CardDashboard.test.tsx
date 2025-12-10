// frontend/tests/components/CardDashboard.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { Provider } from 'react-redux';
import { store } from '../../src/store/store';
import CardDashboard from '../../src/components/CardDashboard';
import { server } from '../setupTests';
import { http, HttpResponse } from 'msw';

describe('CardDashboard', () => {
  beforeEach(() => {
    server.use(
      http.get('*/cards/real', ({ request }) => {
        const url = new URL(request.url);
        const accountId = url.searchParams.get('accountId');
        if (accountId === 'acc123') {
          return HttpResponse.json({
            cardId: 'REAL#acc123',
            accountId: 'acc123',
            cardNumber: '4532123456789012',
            cardHolderName: 'JOHN DOE',
            expiryDate: '12/28',
            status: 'ACTIVE',
          });
        }
        return new HttpResponse(null, { status: 404 });
      }),
      http.get('*/cards/vcns', ({ request }) => {
        const url = new URL(request.url);
        const realCardId = url.searchParams.get('realCardId');
        if (realCardId === 'REAL#acc123') {
          return HttpResponse.json([
            {
              tokenId: 'VCN#1',
              realCardId: 'REAL#acc123',
              vcn: '4532 1234 5678 9012',
              spendLimit: 500,
              expiresAt: new Date().toISOString(),
              status: 'ACTIVE',
            },
          ]);
        }
        return HttpResponse.json([]);
      })
    );
  });

  it('loads card and VCNs when valid account ID is entered', async () => {
    render(
      <Provider store={store}>
        <CardDashboard />
      </Provider>
    );

    await userEvent.type(screen.getByPlaceholderText(/Enter Account ID/i), 'acc123');
    await userEvent.click(screen.getByText('Load Card'));

    await waitFor(() => {
      expect(screen.getByText('**** **** **** 9012')).toBeInTheDocument();
      expect(screen.getByText('JOHN DOE')).toBeInTheDocument();
      expect(screen.getByText('4532 1234 5678 9012')).toBeInTheDocument();
    });
  });

  it('shows error for invalid account ID', async () => {
    render(
      <Provider store={store}>
        <CardDashboard />
      </Provider>
    );

    await userEvent.type(screen.getByPlaceholderText(/Enter Account ID/i), 'invalid');
    await userEvent.click(screen.getByText('Load Card'));

    await waitFor(() => {
    expect(screen.getByText('Failed to fetch real card', {
      selector: '.text-red-600.text-center.bg-red-50'
    })).toBeInTheDocument();
  });
  });

  it('requires account ID before loading', () => {
    render(
      <Provider store={store}>
        <CardDashboard />
      </Provider>
    );

    expect(screen.getByPlaceholderText(/Enter Account ID/i)).toBeInTheDocument();
    expect(screen.queryByText(/Your Card/i)).not.toBeInTheDocument();
  });
});