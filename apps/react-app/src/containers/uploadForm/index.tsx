"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import ReactCrop, { Crop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import { ApiJavalin } from "@/api";

const UploadForm = () => {
  const router = useRouter();
  const [src, setSrc] = useState<string | null>(null);
  const [originalFile, setOriginalFile] = useState<File | null>(null);
  const [crop, setCrop] = useState<Crop | null>(null);
  const [imageElement, setImageElement] = useState<HTMLImageElement | null>(
    null
  );
  const [showManualInput, setShowManualInput] = useState(false);
  const [x, setX] = useState<number>(0);
  const [y, setY] = useState<number>(0);
  const [width, setWidth] = useState<number>(0);
  const [height, setHeight] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);

  const handleCropChange = (newCrop: Crop) => {
    setCrop(newCrop);
  };

  const handleSelectFile = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      const file = event.target.files[0];

      // Reset previous state
      setError(null);
      setSrc(null);
      setOriginalFile(null);
      setCrop(null);
      setShowManualInput(false);

      // Validate file size (max 2GB)
      const maxSize = 2 * 1024 * 1024 * 1024; // 2GB in bytes
      if (file.size > maxSize) {
        setError(
          `File is too large. Maximum size is 2GB. Your file is ${(
            file.size /
            (1024 * 1024)
          ).toFixed(2)}MB.`
        );
        return;
      }

      // Validate file type (only common web formats)
      const allowedTypes = [
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/bmp",
        "image/gif",
      ];
      if (!allowedTypes.includes(file.type)) {
        setError(
          `Invalid file format. Please use JPG, PNG, BMP, or GIF. Your file is ${
            file.type || "unknown format"
          }.`
        );
        return;
      }

      setOriginalFile(file);

      // Try to load image for preview
      const reader = new FileReader();
      reader.addEventListener("load", () => setSrc(reader.result as string));
      reader.addEventListener("error", () => {
        // If image fails to load, switch to manual input
        setShowManualInput(true);
      });
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = () => {
    if (!originalFile) {
      setError("Please select a file.");
      return;
    }

    let actualX, actualY, actualWidth, actualHeight;

    if (showManualInput || !crop) {
      // Use manual input values
      if (width <= 0 || height <= 0) {
        setError("Please enter valid crop dimensions.");
        return;
      }
      actualX = x;
      actualY = y;
      actualWidth = width;
      actualHeight = height;
    } else {
      // Use crop from ReactCrop
      if (!imageElement) {
        setError("Image not loaded properly.");
        return;
      }

      // Get the natural (actual) image dimensions
      const naturalWidth = imageElement.naturalWidth;
      const naturalHeight = imageElement.naturalHeight;

      // Get the displayed image dimensions
      const displayedWidth = imageElement.width;
      const displayedHeight = imageElement.height;

      // Calculate scale factors
      const scaleX = naturalWidth / displayedWidth;
      const scaleY = naturalHeight / displayedHeight;

      // Convert crop coordinates from displayed size to actual image size
      actualX = Math.round(crop.x * scaleX);
      actualY = Math.round(crop.y * scaleY);
      actualWidth = Math.round(crop.width * scaleX);
      actualHeight = Math.round(crop.height * scaleY);
    }

    // create form data to send to backend
    const form = new FormData();
    form.append("file", originalFile);
    form.append("x", String(actualX));
    form.append("y", String(actualY));
    form.append("width", String(actualWidth));
    form.append("height", String(actualHeight));

    ApiJavalin({
      headers: { "Content-Type": "multipart/form-data" },
    })
      .post("/upload-and-process", form)
      .then((response) => {
        router.push("/upload/result?uploadId=" + response.data);
      })
      .catch((error) => {
        alert("Error sending the image. Check the console for details.");
        console.error("Error sending the image:", error);
      });
  };

  return (
    <div className="container-fluid d-flex justify-content-center align-items-center min-vh-100">
      <div
        className="card shadow-lg"
        style={{ maxWidth: "600px", width: "100%" }}
      >
        <div className="card-header text-center bg-primary text-white">
          <h2 className="mb-0">Upload Image to be Processed</h2>
        </div>
        <div className="card-body p-4">
          <div className="mb-3">
            <label htmlFor="fileInput" className="form-label fw-bold">
              Select Image:
            </label>
            <input
              id="fileInput"
              type="file"
              accept="image/jpeg,image/jpg,image/png,image/bmp,image/gif"
              onChange={handleSelectFile}
              className="form-control"
            />
            <small className="text-muted">
              Max size: 2GB. Formats: JPG, PNG, BMP, GIF
            </small>
          </div>

          {error && (
            <div className="alert alert-danger" role="alert">
              <i className="bi bi-exclamation-triangle me-2"></i>
              {error}
            </div>
          )}

          {originalFile && (
            <div className="text-center">
              <div className="alert alert-success mb-3" role="alert">
                <i className="bi bi-check-circle me-2"></i>
                <strong>File selected:</strong> {originalFile.name} (
                {(originalFile.size / (1024 * 1024)).toFixed(2)} MB)
              </div>

              {!showManualInput && src ? (
                <>
                  <div className="alert alert-info mb-3" role="alert">
                    <i className="bi bi-info-circle me-2"></i>
                    <strong>Instructions:</strong> Select the area of the image
                    you want to process by dragging a rectangle with the mouse,
                    then press the &quot;Submit&quot; button.
                    <button
                      className="btn btn-link btn-sm ms-2"
                      onClick={() => setShowManualInput(true)}
                    >
                      Switch to manual input
                    </button>
                  </div>
                  <div className="mb-3">
                    <ReactCrop crop={crop!} onChange={handleCropChange}>
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        ref={(ref) => setImageElement(ref)}
                        src={src}
                        alt="Image for crop"
                        style={{ maxWidth: "100%" }}
                        onError={() => setShowManualInput(true)}
                      />
                    </ReactCrop>
                  </div>
                  {crop && (
                    <div className="alert alert-secondary mb-3">
                      <h6 className="mb-2">
                        <i className="bi bi-crop me-2"></i>
                        Coords selected:
                      </h6>
                      <div className="row text-start">
                        <div className="col-6">
                          <small>
                            <strong>X:</strong> {Math.round(crop.x)}px
                            <br />
                            <strong>Y:</strong> {Math.round(crop.y)}px
                          </small>
                        </div>
                        <div className="col-6">
                          <small>
                            <strong>Width:</strong> {Math.round(crop.width)}px
                            <br />
                            <strong>Height:</strong> {Math.round(crop.height)}px
                          </small>
                        </div>
                      </div>
                    </div>
                  )}
                  <button
                    onClick={handleSubmit}
                    className="btn btn-success btn-lg"
                    disabled={!crop || crop.height === 0 || crop.width === 0}
                  >
                    <i className="bi bi-send me-2"></i>
                    Submit
                  </button>
                </>
              ) : (
                <>
                  <div className="alert alert-info mb-3" role="alert">
                    <i className="bi bi-info-circle me-2"></i>
                    <strong>Manual Input Mode:</strong> Enter the crop
                    coordinates and dimensions in pixels, then press the
                    &quot;Submit&quot; button.
                    {src && (
                      <button
                        className="btn btn-link btn-sm ms-2"
                        onClick={() => setShowManualInput(false)}
                      >
                        Switch to visual crop
                      </button>
                    )}
                  </div>

                  <div className="row mb-3">
                    <div className="col-6">
                      <label htmlFor="xInput" className="form-label fw-bold">
                        X (pixels):
                      </label>
                      <input
                        id="xInput"
                        type="number"
                        min="0"
                        value={x}
                        onChange={(e) => setX(Number(e.target.value))}
                        className="form-control"
                        placeholder="Starting X position"
                      />
                    </div>
                    <div className="col-6">
                      <label htmlFor="yInput" className="form-label fw-bold">
                        Y (pixels):
                      </label>
                      <input
                        id="yInput"
                        type="number"
                        min="0"
                        value={y}
                        onChange={(e) => setY(Number(e.target.value))}
                        className="form-control"
                        placeholder="Starting Y position"
                      />
                    </div>
                  </div>

                  <div className="row mb-3">
                    <div className="col-6">
                      <label
                        htmlFor="widthInput"
                        className="form-label fw-bold"
                      >
                        Width (pixels):
                      </label>
                      <input
                        id="widthInput"
                        type="number"
                        min="1"
                        value={width}
                        onChange={(e) => setWidth(Number(e.target.value))}
                        className="form-control"
                        placeholder="Crop width"
                      />
                    </div>
                    <div className="col-6">
                      <label
                        htmlFor="heightInput"
                        className="form-label fw-bold"
                      >
                        Height (pixels):
                      </label>
                      <input
                        id="heightInput"
                        type="number"
                        min="1"
                        value={height}
                        onChange={(e) => setHeight(Number(e.target.value))}
                        className="form-control"
                        placeholder="Crop height"
                      />
                    </div>
                  </div>

                  <button
                    onClick={handleSubmit}
                    className="btn btn-success btn-lg"
                    disabled={width <= 0 || height <= 0}
                  >
                    <i className="bi bi-send me-2"></i>
                    Submit
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UploadForm;
