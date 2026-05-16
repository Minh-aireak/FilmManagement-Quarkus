import React, { createContext, useContext, useState, type ReactNode } from 'react';
import { type Movie, type Showtime, type Room, type Account } from '../types';

interface FilmListCache {
  movies: Movie[];
  page: number;
  hasMore: boolean;
}

interface AdminFilmsCache {
  films: Movie[];
  page: number;
  hasMore: boolean;
}

interface AdminShowtimesCache {
  showtimes: Showtime[];
  movies: Movie[];
  rooms: Room[];
  page: number;
  hasMore: boolean;
  timeFilter: 'upcoming' | 'past';
}

interface FilmWithShowtimes extends Movie {
  showtimes: Showtime[];
}

interface ShowtimesPageCache {
  [date: string]: {
    films: FilmWithShowtimes[];
    page: number;
    hasMore: boolean;
  };
}

interface CacheContextType {
  filmListCache: FilmListCache | null;
  setFilmListCache: (data: FilmListCache | null) => void;
  adminFilmsCache: AdminFilmsCache | null;
  setAdminFilmsCache: (data: AdminFilmsCache | null) => void;
  adminShowtimesCache: AdminShowtimesCache | null;
  setAdminShowtimesCache: (data: AdminShowtimesCache | null) => void;
  showtimesPageCache: ShowtimesPageCache;
  setShowtimesPageCache: (date: string, data: { films: FilmWithShowtimes[], page: number, hasMore: boolean } | null) => void;
  profileCache: Account | null;
  setProfileCache: (data: Account | null) => void;
  clearCache: () => void;
}

const CacheContext = createContext<CacheContextType | undefined>(undefined);

export const CacheProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [filmListCache, setFilmListCache] = useState<FilmListCache | null>(null);
  const [adminFilmsCache, setAdminFilmsCache] = useState<AdminFilmsCache | null>(null);
  const [adminShowtimesCache, setAdminShowtimesCache] = useState<AdminShowtimesCache | null>(null);
  const [showtimesPageCache, setShowtimesPageCacheInternal] = useState<ShowtimesPageCache>({});
  const [profileCache, setProfileCache] = useState<Account | null>(null);

  const setShowtimesPageCache = (date: string, data: { films: FilmWithShowtimes[], page: number, hasMore: boolean } | null) => {
    setShowtimesPageCacheInternal(prev => {
      if (data === null) {
        const newCache = { ...prev };
        delete newCache[date];
        return newCache;
      }
      return {
        ...prev,
        [date]: data
      };
    });
  };

  const clearCache = () => {
    setFilmListCache(null);
    setAdminFilmsCache(null);
    setAdminShowtimesCache(null);
    setShowtimesPageCacheInternal({});
    setProfileCache(null);
  };

  return (
    <CacheContext.Provider value={{
      filmListCache,
      setFilmListCache,
      adminFilmsCache,
      setAdminFilmsCache,
      adminShowtimesCache,
      setAdminShowtimesCache,
      showtimesPageCache,
      setShowtimesPageCache,
      profileCache,
      setProfileCache,
      clearCache
    }}>
      {children}
    </CacheContext.Provider>
  );
};

export const useCache = () => {
  const context = useContext(CacheContext);
  if (context === undefined) {
    throw new Error('useCache must be used within a CacheProvider');
  }
  return context;
};
