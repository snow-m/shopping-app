/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huohoubrowser.zxing.multi;

import com.huohoubrowser.zxing.BinaryBitmap;
import com.huohoubrowser.zxing.DecodeHintType;
import com.huohoubrowser.zxing.NotFoundException;
import com.huohoubrowser.zxing.Reader;
import com.huohoubrowser.zxing.ReaderException;
import com.huohoubrowser.zxing.Result;
import com.huohoubrowser.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
 * After one barcode is found, the areas left, above, right and below the barcode's
 * {@link ResultPoint}s are scanned, recursively.</p>
 *
 * <p>A caller may want to also employ {@link ByQuadrantReader} when attempting to find multiple
 * 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might prevent
 * detecting any one of them.</p>
 *
 * <p>That is, instead of passing a {@link Reader} a caller might pass
 * {@code new ByQuadrantReader(reader)}.</p>
 *
 * @author Sean Owen
 */
public final class GenericMultipleBarcodeReader implements MultipleBarcodeReader {

  private static final int MIN_DIMENSION_TO_RECUR = 100;

  private final Reader delegate;

  public GenericMultipleBarcodeReader(Reader delegate) {
    this.delegate = delegate;
  }

  @Override
  public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
    return decodeMultiple(image, null);
  }

  @Override
  public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType,?> hints)
      throws NotFoundException {
    List<Result> results = new ArrayList<Result>();
    doDecodeMultiple(image, hints, results, 0, 0);
    if (results.isEmpty()) {
      throw NotFoundException.getNotFoundInstance();
    }
    return results.toArray(new Result[results.size()]);
  }

  private void doDecodeMultiple(BinaryBitmap image,
                                Map<DecodeHintType,?> hints,
                                List<Result> results,
                                int xOffset,
                                int yOffset) {
    Result result;
    try {
      result = delegate.decode(image, hints);
    } catch (ReaderException re) {
      return;
    }
    boolean alreadyFound = false;
    for (Result existingResult : results) {
      if (existingResult.getText().equals(result.getText())) {
        alreadyFound = true;
        break;
      }
    }
    if (alreadyFound) {
      return;
    }
    results.add(translateResultPoints(result, xOffset, yOffset));
    ResultPoint[] resultPoints = result.getResultPoints();
    if (resultPoints == null || resultPoints.length == 0) {
      return;
    }
    int width = image.getWidth();
    int height = image.getHeight();
    float minX = width;
    float minY = height;
    float maxX = 0.0f;
    float maxY = 0.0f;
    for (ResultPoint point : resultPoints) {
      float x = point.getX();
      float y = point.getY();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    // Decode left of barcode
    if (minX > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, (int) minX, height),
                       hints, results, xOffset, yOffset);
    }
    // Decode above barcode
    if (minY > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, width, (int) minY),
                       hints, results, xOffset, yOffset);
    }
    // Decode right of barcode
    if (maxX < width - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop((int) maxX, 0, width - (int) maxX, height),
                       hints, results, xOffset + (int) maxX, yOffset);
    }
    // Decode below barcode
    if (maxY < height - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, (int) maxY, width, height - (int) maxY),
                       hints, results, xOffset, yOffset + (int) maxY);
    }
  }

  private static Result translateResultPoints(Result result, int xOffset, int yOffset) {
    ResultPoint[] oldResultPoints = result.getResultPoints();
    if (oldResultPoints == null) {
      return result;
    }
    ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.length];
    for (int i = 0; i < oldResultPoints.length; i++) {
      ResultPoint oldPoint = oldResultPoints[i];
      newResultPoints[i] = new ResultPoint(oldPoint.getX() + xOffset, oldPoint.getY() + yOffset);
    }
    return new Result(result.getText(), result.getRawBytes(), newResultPoints,
        result.getBarcodeFormat());
  }

}
