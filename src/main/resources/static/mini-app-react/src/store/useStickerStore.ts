import { create } from 'zustand';
import { StickerSetResponse, AuthResponse } from '@/types/sticker';

interface StickerState {
  // Состояние загрузки
  isLoading: boolean;
  isAuthLoading: boolean;
  
  // Данные
  stickerSets: StickerSetResponse[];
  authStatus: AuthResponse | null;
  
  // Ошибки
  error: string | null;
  authError: string | null;
  
  // Действия
  setLoading: (loading: boolean) => void;
  setAuthLoading: (loading: boolean) => void;
  setStickerSets: (stickerSets: StickerSetResponse[]) => void;
  setAuthStatus: (authStatus: AuthResponse) => void;
  setError: (error: string | null) => void;
  setAuthError: (error: string | null) => void;
  removeStickerSet: (id: number) => void;
  clearErrors: () => void;
}

export const useStickerStore = create<StickerState>((set, get) => ({
  // Начальное состояние
  isLoading: false,
  isAuthLoading: false,
  stickerSets: [],
  authStatus: null,
  error: null,
  authError: null,

  // Действия
  setLoading: (loading: boolean) => set({ isLoading: loading }),
  setAuthLoading: (loading: boolean) => set({ isAuthLoading: loading }),
  
  setStickerSets: (stickerSets: StickerSetResponse[]) => set({ stickerSets }),
  
  setAuthStatus: (authStatus: AuthResponse) => set({ authStatus }),
  
  setError: (error: string | null) => set({ error }),
  setAuthError: (authError: string | null) => set({ authError }),
  
  removeStickerSet: (id: number) => {
    const { stickerSets } = get();
    const updatedStickerSets = stickerSets.filter(stickerSet => stickerSet.id !== id);
    set({ stickerSets: updatedStickerSets });
  },
  
  clearErrors: () => set({ error: null, authError: null }),
}));
