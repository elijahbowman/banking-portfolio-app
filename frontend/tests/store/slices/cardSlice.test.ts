import { describe, it, expect, beforeEach } from 'vitest';
import { store } from '../../../src/store/store';
import { fetchRealCard, fetchVCNs, createVCN } from '../../../src/store/slices/cardSlice';
import { server } from '../../setupTests';
import { http, HttpResponse } from 'msw';

describe('cardSlice', () => {
    beforeEach(() => {
    store.dispatch({ type: 'card/resetCard' });  // ← RESET STATE
    server.use(
        http.get('*/cards/real', () => {
            return HttpResponse.json({
            cardId: 'REAL#acc123',
            accountId: 'acc123',
            cardNumber: '4532123456789012',
            cardHolderName: 'JOHN DOE',
            expiryDate: '12/28',
            status: 'ACTIVE',
            });
        }),
        http.get('*/cards/vcns', () => {
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
        }),
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
    
    it('fetches real card', async () => {
        await store.dispatch(fetchRealCard('acc123'));
        const state = store.getState().card;
        expect(state.realCard?.cardId).toBe('REAL#acc123');
        expect(state.realCardStatus).toBe('succeeded');
    });

    it('fetches VCNs', async () => {
        await store.dispatch(fetchVCNs('REAL#acc123'));
        const state = store.getState().card;
        expect(state.vcns).toHaveLength(1);
        expect(state.vcns[0].tokenId).toBe('VCN#1');
    });

    it('creates VCN', async () => {
        await store.dispatch(createVCN({ realCardId: 'REAL#acc123', limit: 750 }));
        const state = store.getState().card;
        expect(state.vcns).toHaveLength(1);
        expect(state.vcns[0].tokenId).toBe('VCN#new');
    });
});