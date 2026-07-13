import type { Metadata } from 'next';
import { Inter, JetBrains_Mono } from 'next/font/google';
import './globals.css';
import { Providers } from '@/components/Providers';

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ['latin'],
  variable: '--font-jetbrains',
  display: 'swap',
});

export const metadata: Metadata = {
  title: {
    default: 'TradeForge — Electronic Trading Platform',
    template: '%s | TradeForge',
  },
  description:
    'A price-time priority electronic exchange with real-time order book, limit orders, and WebSocket market data.',
  keywords: ['trading', 'exchange', 'order book', 'matching engine', 'limit orders'],
  authors: [{ name: 'TradeForge Team' }],
  robots: 'noindex, nofollow',
  openGraph: {
    title: 'TradeForge — Electronic Trading Platform',
    description: 'Real-time electronic trading platform with price-time priority matching.',
    type: 'website',
  },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${inter.variable} ${jetbrainsMono.variable} dark`}>
      <body className="bg-surface text-slate-100 antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
