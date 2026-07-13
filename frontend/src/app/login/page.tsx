'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Zap, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';
import { authApi } from '@/lib/api';
import { saveAuth } from '@/lib/auth';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/api-client';
import { Alert } from '@/components/ui/Alert';

const schema = z.object({
  email:    z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router        = useRouter();
  const { refreshUser } = useAuth();
  const [showPwd, setShowPwd] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      saveAuth(data);
      refreshUser();
      router.push('/dashboard');
    },
  });

  const onSubmit = (data: FormData) => mutation.mutate(data);

  return (
    <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden">
      {/* Background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-600/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md animate-slide-up">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center shadow-glow-brand mb-4">
            <Zap className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gradient">TradeForge</h1>
          <p className="text-slate-500 text-sm mt-1">Electronic Trading Platform</p>
        </div>

        <div className="card p-8">
          <h2 className="text-xl font-semibold text-slate-100 mb-6">Sign in to your account</h2>

          {mutation.isError && (
            <Alert variant="error" message={getErrorMessage(mutation.error)} className="mb-4" />
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Email */}
            <div>
              <label className="label">Email address</label>
              <input
                type="email"
                placeholder="you@example.com"
                className={`input ${errors.email ? 'input-error' : ''}`}
                {...register('email')}
              />
              {errors.email && (
                <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'}
                  placeholder="••••••••"
                  className={`input pr-10 ${errors.password ? 'input-error' : ''}`}
                  {...register('password')}
                />
                <button
                  type="button"
                  onClick={() => setShowPwd((p) => !p)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                >
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>
              )}
            </div>

            <button
              type="submit"
              className="btn-primary w-full mt-2"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            {"Don't have an account? "}
            <Link href="/register" className="text-brand-400 hover:text-brand-300 font-medium">
              Register
            </Link>
          </p>

          {/* Demo credentials hint */}
          <div className="mt-6 p-3 rounded-lg bg-surface-tertiary border border-surface-border text-xs text-slate-400 space-y-1">
            <p className="font-medium text-slate-300">Demo accounts</p>
            <p>Buyer: <span className="text-brand-300 font-mono">alice@tradeforge.local</span> / <span className="font-mono">Trader@1234</span></p>
            <p>Seller: <span className="text-brand-300 font-mono">bob@tradeforge.local</span> / <span className="font-mono">Trader@1234</span></p>
            <p>Admin: <span className="text-brand-300 font-mono">admin@tradeforge.local</span> / <span className="font-mono">Admin@1234</span></p>
          </div>
        </div>
      </div>
    </div>
  );
}
