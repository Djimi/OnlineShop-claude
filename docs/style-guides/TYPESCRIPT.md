# TypeScript & React Style Guide

Based on industry best practices for React 19 with TypeScript.

## TypeScript Configuration

### Strict Mode Required
```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}
```

## Naming Conventions

### Files
- Components: `PascalCase.tsx` (e.g., `ItemCard.tsx`)
- Hooks: `camelCase.ts` starting with `use` (e.g., `useAuth.ts`)
- Utilities: `camelCase.ts` (e.g., `formatCurrency.ts`)
- Types: `camelCase.ts` or colocated in component file
- Stores: `camelCase.ts` ending with `Store` (e.g., `authStore.ts`)

### Components
```tsx
// Component names: PascalCase
export function ItemCard({ item }: ItemCardProps) { }

// Props interfaces: ComponentNameProps
interface ItemCardProps {
  item: Item;
  onSelect?: (item: Item) => void;
}
```

### Variables and Functions
```tsx
// camelCase for variables and functions
const itemCount = items.length;
const handleClick = () => { };

// SCREAMING_SNAKE_CASE for constants
const MAX_ITEMS_PER_PAGE = 20;
const API_BASE_URL = import.meta.env.VITE_API_URL;
```

## React Components

### Functional Components Only
```tsx
// Good - functional component
export function ItemCard({ item, onSelect }: ItemCardProps) {
  return (
    <div onClick={() => onSelect?.(item)}>
      {item.name}
    </div>
  );
}

// Bad - class component (don't use)
class ItemCard extends React.Component { }
```

### Props Interface Definition
```tsx
// Define props interface above component
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  children: React.ReactNode;
  onClick?: () => void;
}

export function Button({
  variant = 'primary',
  size = 'md',
  disabled = false,
  children,
  onClick,
}: ButtonProps) {
  // Implementation
}
```

### Component Structure
Order within a component:
1. Props destructuring
2. Hooks (useState, useEffect, custom hooks)
3. Derived state / computations
4. Event handlers
5. Return JSX

```tsx
export function ItemList({ categoryId }: ItemListProps) {
  // 1. Hooks
  const [searchTerm, setSearchTerm] = useState('');
  const { data: items, isLoading } = useItems(categoryId);

  // 2. Derived state
  const filteredItems = items?.filter(item =>
    item.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // 3. Event handlers
  const handleSearch = (term: string) => {
    setSearchTerm(term);
  };

  // 4. Early returns for loading/error states
  if (isLoading) return <LoadingSpinner />;

  // 5. Main render
  return (
    <div>
      <SearchInput onSearch={handleSearch} />
      <ItemGrid items={filteredItems} />
    </div>
  );
}
```

## Hooks

### Custom Hooks
- Always prefix with `use`
- Return typed objects, not arrays (unless order matters like useState)

```tsx
// Good - object return
export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const login = async (credentials: Credentials) => { };
  const logout = () => { };

  return { user, isLoading, login, logout };
}

// Usage is clear
const { user, login, logout } = useAuth();
```

### useEffect Dependencies
- Always include all dependencies
- Use exhaustive-deps ESLint rule

```tsx
// Good - all dependencies listed
useEffect(() => {
  fetchItems(categoryId);
}, [categoryId, fetchItems]);

// Bad - missing dependency
useEffect(() => {
  fetchItems(categoryId);
}, []); // categoryId missing!
```

### Avoid useEffect for Derived State
```tsx
// Bad - unnecessary effect
const [fullName, setFullName] = useState('');
useEffect(() => {
  setFullName(`${firstName} ${lastName}`);
}, [firstName, lastName]);

// Good - computed directly
const fullName = `${firstName} ${lastName}`;

// Good - with useMemo for expensive computations
const sortedItems = useMemo(
  () => items.sort((a, b) => a.price - b.price),
  [items]
);
```

## State Management

### Zustand Store Pattern
```tsx
// stores/authStore.ts
import { create } from 'zustand';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (user: User, token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  login: (user, token) => set({
    user,
    token,
    isAuthenticated: true,
  }),

  logout: () => set({
    user: null,
    token: null,
    isAuthenticated: false,
  }),
}));
```

### React Query for Server State
```tsx
// Good - use React Query for server data
export function useItems(categoryId: string) {
  return useQuery({
    queryKey: ['items', categoryId],
    queryFn: () => itemsService.getByCategory(categoryId),
  });
}

// Usage
const { data: items, isLoading, error } = useItems(categoryId);
```

## TypeScript Patterns

### Type vs Interface
- Use `interface` for object shapes (extendable)
- Use `type` for unions, intersections, primitives

```tsx
// Interface for objects
interface User {
  id: string;
  email: string;
  name: string;
}

// Type for unions
type Status = 'pending' | 'approved' | 'rejected';
type Result<T> = Success<T> | Error;

// Type for mapped types
type Readonly<T> = { readonly [K in keyof T]: T[K] };
```

### Avoid `any`
```tsx
// Bad
const handleData = (data: any) => { };

// Good - use unknown and narrow
const handleData = (data: unknown) => {
  if (isUser(data)) {
    console.log(data.email);
  }
};

// Good - generic when type varies
const handleData = <T>(data: T) => { };
```

### Discriminated Unions for States
```tsx
type RequestState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: string };

function renderState<T>(state: RequestState<T>) {
  switch (state.status) {
    case 'idle':
      return null;
    case 'loading':
      return <Spinner />;
    case 'success':
      return <DataView data={state.data} />;
    case 'error':
      return <ErrorMessage message={state.error} />;
  }
}
```

## JSX Best Practices

### Conditional Rendering
```tsx
// Good - ternary for simple conditions
{isLoggedIn ? <UserMenu /> : <LoginButton />}

// Good - && for show/hide (be careful with 0)
{items.length > 0 && <ItemList items={items} />}

// Bad - && with numbers (renders "0")
{count && <Counter count={count} />}

// Good - explicit boolean
{count > 0 && <Counter count={count} />}
```

### Lists and Keys
```tsx
// Good - stable, unique keys
{items.map(item => (
  <ItemCard key={item.id} item={item} />
))}

// Bad - index as key (causes issues on reorder)
{items.map((item, index) => (
  <ItemCard key={index} item={item} />
))}
```

### Event Handlers
```tsx
// Good - inline for simple handlers
<button onClick={() => setOpen(true)}>Open</button>

// Good - named function for complex logic
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  await submitForm(formData);
};

<form onSubmit={handleSubmit}>
```

## Styling with Tailwind

### Class Organization
Order classes logically:
1. Layout (flex, grid, position)
2. Sizing (w, h, p, m)
3. Typography (text, font)
4. Colors (bg, text, border)
5. Effects (shadow, opacity)
6. States (hover, focus)

```tsx
// Organized
<button className="flex items-center justify-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg shadow-md hover:bg-blue-700 focus:ring-2">

// Use cn() utility for conditional classes
import { cn } from '@/lib/utils';

<button className={cn(
  "px-4 py-2 rounded-lg font-medium",
  variant === 'primary' && "bg-blue-600 text-white",
  variant === 'secondary' && "bg-gray-200 text-gray-800",
  disabled && "opacity-50 cursor-not-allowed"
)}>
```

## File Organization

```
src/
├── components/
│   ├── common/           # Reusable UI components
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   └── Card.tsx
│   ├── layout/           # Layout components
│   │   ├── Navbar.tsx
│   │   └── Footer.tsx
│   └── features/         # Feature-specific components
│       ├── items/
│       │   ├── ItemCard.tsx
│       │   └── ItemList.tsx
│       └── auth/
│           ├── LoginForm.tsx
│           └── RegisterForm.tsx
├── hooks/                # Custom hooks
│   ├── useAuth.ts
│   └── useItems.ts
├── services/             # API services
│   ├── api.ts
│   ├── authService.ts
│   └── itemsService.ts
├── stores/               # Zustand stores
│   └── authStore.ts
├── types/                # Type definitions
│   └── index.ts
├── utils/                # Utility functions
│   └── formatters.ts
└── pages/                # Route pages
    ├── Home.tsx
    ├── Login.tsx
    └── Items.tsx
```

## Error Handling

### API Errors
```tsx
// Service layer
export async function fetchItems(): Promise<Item[]> {
  const response = await api.get<Item[]>('/items');
  return response.data;
}

// Component with React Query
const { data, error, isLoading } = useQuery({
  queryKey: ['items'],
  queryFn: fetchItems,
});

if (error) {
  return <ErrorMessage message={error.message} />;
}
```

### Error Boundaries
```tsx
// For catching render errors
<ErrorBoundary fallback={<ErrorFallback />}>
  <ItemList />
</ErrorBoundary>
```
