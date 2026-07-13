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

const schema = z
  .object({
    name:            z.string().min(2, 'Name must be at least 2 characters'),
    email:           z.string().email('Enter a valid email'),
    password:        z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Must contain an uppercase letter')
      .regex(/[a-z]/, 'Must contain a lowercase letter')
      .regex(/\d/,    'Must contain a digit')
      .regex(/[^a-zA-Z\d]/, 'Must contain a special character'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormData = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const { refreshUser } = useAuth();
  const [showPwd, setShowPwd] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const mutation = useMutation({
    mutationFn: (data: { name: string; email: string; password: string }) =>
      authApi.register(data),
    onSuccess: (data) => {
      saveAuth(data);
      refreshUser();
      router.push('/dashboard');
    },
  });

  const onSubmit = ({ name, email, password }: FormData) =>
    mutation.mutate({ name, email, password });

  return (
    <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden py-12">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-brand-600/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md animate-slide-up">
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center shadow-glow-brand mb-4">
            <Zap className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gradient">TradeForge</h1>
          <p className="text-slate-500 text-sm mt-1">Create your trading account</p>
        </div>

        <div className="card p-8">
          <h2 className="text-xl font-semibold text-slate-100 mb-6">Create Account</h2>

          {mutation.isError && (
            <Alert variant="error" message={getErrorMessage(mutation.error)} className="mb-4" />
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="label">Full Name</label>
              <input type="text" placeholder="Alice Trader" className={`input ${errors.name ? 'input-error' : ''}`} {...register('name')} />
              {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name.message}</p>}
            </div>

            <div>
              <label className="label">Email address</label>
              <input type="email" placeholder="you@example.com" className={`input ${errors.email ? 'input-error' : ''}`} {...register('email')} />
              {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'}
                  placeholder="Min 8 chars, uppercase, digit, special"
                  className={`input pr-10 ${errors.password ? 'input-error' : ''}`}
                  {...register('password')}
                />
                <button type="button" onClick={() => setShowPwd((p) => !p)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
            </div>

            <div>
              <label className="label">Confirm Password</label>
              <input type="password" placeholder="Repeat your password" className={`input ${errors.confirmPassword ? 'input-error' : ''}`} {...register('confirmPassword')} />
              {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
            </div>

            <button type="submit" className="btn-primary w-full mt-2" disabled={mutation.isPending}>
              {mutation.isPending ? 'Creating account…' : 'Create Account'}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            Already have an account?{' '}
            <Link href="/login" className="text-brand-400 hover:text-brand-300 font-medium">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
