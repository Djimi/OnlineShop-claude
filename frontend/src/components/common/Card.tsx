import type { ReactNode, HTMLAttributes } from 'react';
import { clsx } from 'clsx';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  hoverable?: boolean;
}

export function Card({ children, hoverable = false, className, ...rest }: CardProps) {
  return (
    <div
      className={clsx(
        'card p-6',
        hoverable && 'cursor-pointer transform hover:scale-105 transition-transform',
        className
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
