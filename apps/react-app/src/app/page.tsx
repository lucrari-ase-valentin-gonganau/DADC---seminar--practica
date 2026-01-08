"use client";
import { useRouter } from "next/navigation";

export default function Home() {
  const router = useRouter();

  return (
    <div className="container py-5">
      <div
        className="card shadow-sm"
        style={{ maxWidth: 500, margin: "0 auto" }}
      >
        <div className="card-body text-center">
          <h5 className="card-title mb-3">Project DAD</h5>
          <p className="card-text text-muted mb-4">
            Choose one of the actions:
          </p>

          <div className="d-flex justify-content-around">
            <button
              className="btn btn-primary"
              onClick={() => router.push("/upload")}
            >
              Upload Image
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => router.push("/smtp")}
            >
              SMTP
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
