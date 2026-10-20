import { useState } from 'react';
import { InsightList } from '../components/InsightList';
import { Card } from '../components/ui/Card';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { insightService } from '../services/insightService';
import { currentYearMonth, formatDate, monthLabel } from '../utils/dates';

export function InsightsPage() {
  const { user } = useAuth();
  const [month, setMonth] = useState(currentYearMonth);
  const insights = useAsync(() => insightService.forMonth(month.year, month.month, user?.preferredCurrency), [month.year, month.month]);

  return (
    <>
      <PageHeader
        title="Insights"
        description="Plain-language observations about your money, with the numbers behind each one."
        actions={<MonthPicker value={month} onChange={setMonth} />}
      />
      {insights.loading && !insights.data ? (
        <LoadingState />
      ) : insights.error ? (
        <ErrorState error={insights.error} onRetry={insights.reload} />
      ) : insights.data ? (
        <div className="flex flex-col gap-4">
          {insights.data.insights.length === 0 ? (
            <Card>
              <EmptyState icon="bulb" title={`Nothing to flag for ${monthLabel(month)}`}>
                As you record transactions, set budgets, and save toward goals, Nest will point out anything worth knowing.
              </EmptyState>
            </Card>
          ) : (
            <InsightList insights={insights.data.insights} />
          )}
          <p className="text-xs text-ink-500">
            Based on data up to {formatDate(insights.data.asOf)} in {insights.data.currency}. {insights.data.disclaimer}
          </p>
        </div>
      ) : null}
    </>
  );
}
