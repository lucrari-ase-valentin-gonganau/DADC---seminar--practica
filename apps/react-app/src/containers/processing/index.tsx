"use client";

import { useSearchParams } from "next/navigation";
import { useWebSocket } from "../../../hooks/useWebSocket";
import ShowImage from "./ShowImage";

import { memo, useRef, useEffect } from "react";

const urlWs = `${
  process.env.BASE_PATH_URL_WEBSOCKET || "ws://localhost:8081"
}/ws/notifications`;

const Processing = () => {
  const searchParams = useSearchParams();
  const uploadIdParam = searchParams.get("uploadId");
  const reloadTriggerRef = useRef(0);
  // Only connect WebSocket when we have an uploadId (meaning user is waiting for results)
  const { isConnected, lastMessage } = useWebSocket(urlWs);

  useEffect(() => {
    if (lastMessage?.status === "IMAGE_READY") {
      reloadTriggerRef.current += 1;
    }
  }, [lastMessage]);

  return (
    <div className="container-fluid d-flex justify-content-center align-items-center min-vh-100">
      <div
        className="card shadow-lg"
        style={{ maxWidth: "600px", width: "100%" }}
      >
        <div className="card-header text-center bg-primary text-white">
          <h2 className="mb-0">Check result</h2>
        </div>
        <div className="card-body p-4">
          {uploadIdParam && (
            <div className="alert alert-info" role="alert">
              <i className="bi bi-info-circle me-2"></i>UploadId:{" "}
              {uploadIdParam}
            </div>
          )}
          {isConnected ? (
            <>
              <div className="alert alert-success" role="alert">
                <i className="bi bi-check-circle me-2"></i>WebSocket connected.
              </div>

              {lastMessage && lastMessage.status === "IMAGE_READY" ? (
                <>
                  <div className="alert alert-success" role="alert">
                    <i className="bi bi-check-circle me-2"></i>
                    Processing complete! You can now view the result.
                  </div>
                  <ShowImage
                    key={`${lastMessage.rowId}-${
                      // eslint-disable-next-line react-hooks/refs
                      reloadTriggerRef?.current || 0
                    }`}
                    id={lastMessage.rowId}
                  />
                </>
              ) : (
                <>
                  <div className="alert alert-warning" role="alert">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    Processing not yet complete. Please wait...
                  </div>
                  <div className="d-flex justify-content-center my-4">
                    <div className="spinner-border text-primary" role="status">
                      <span className="visually-hidden">Loading...</span>
                    </div>
                  </div>
                </>
              )}
            </>
          ) : (
            <div className="alert alert-danger" role="alert">
              <i className="bi bi-x-circle me-2"></i>WebSocket disconnected.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default memo(Processing);
