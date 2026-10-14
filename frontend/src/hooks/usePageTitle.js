import { useEffect } from 'react';

export function usePageTitle(title) {
  useEffect(() => {
    document.title = `${title} | Rakshak: Healthcare IAM Security Platform`;
  }, [title]);
}
