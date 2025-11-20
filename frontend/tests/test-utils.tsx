// frontend/tests/test-utils.tsx
import { render, type RenderOptions } from '@testing-library/react'
import { Provider } from 'react-redux'
import { store } from '../src/store/store'
import { type ReactElement } from 'react'

const AllTheProviders = ({ children }: { children: React.ReactNode }) => {
  return <Provider store={store}>{children}</Provider>
}

const customRender = (ui: ReactElement, options?: RenderOptions) =>
  render(ui, { wrapper: AllTheProviders, ...options })

export * from '@testing-library/react'
export { customRender as render }