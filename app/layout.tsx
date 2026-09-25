import type { Metadata } from "next";
import "./globals.css";
import { FeedbackChat } from '@automate/feedback-lib/FeedbackChat';

export const metadata: Metadata = {
  title: "trips",
  description: "טיולים — every trip you take, drawn on a hiking map that works with no signal: the marked route, where you are on it, what is ahead, and the climb",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}
        <FeedbackChat issuesPath="/feedback-lib-issues" />
</body>
    </html>
  );
}
