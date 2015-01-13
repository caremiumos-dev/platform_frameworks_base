/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.renderscript;

import android.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * BLAS
 *
 **/
public final class ScriptIntrinsicBLAS extends ScriptIntrinsic {
    private Allocation mLUT;

    private ScriptIntrinsicBLAS(long id, RenderScript rs) {
        super(id, rs);
    }

    private static final int RsBlas_sdsdot = 1;
    private static final int RsBlas_dsdot = 2;
    private static final int RsBlas_sdot = 3;
    private static final int RsBlas_ddot = 4;
    private static final int RsBlas_cdotu_sub = 5;
    private static final int RsBlas_cdotc_sub = 6;
    private static final int RsBlas_zdotu_sub = 7;
    private static final int RsBlas_zdotc_sub = 8;
    private static final int RsBlas_snrm2 = 9;
    private static final int RsBlas_sasum = 10;
    private static final int RsBlas_dnrm2 = 11;
    private static final int RsBlas_dasum = 12;
    private static final int RsBlas_scnrm2 = 13;
    private static final int RsBlas_scasum = 14;
    private static final int RsBlas_dznrm2 = 15;
    private static final int RsBlas_dzasum = 16;
    private static final int RsBlas_isamax = 17;
    private static final int RsBlas_idamax = 18;
    private static final int RsBlas_icamax = 19;
    private static final int RsBlas_izamax = 20;
    private static final int RsBlas_sswap = 21;
    private static final int RsBlas_scopy = 22;
    private static final int RsBlas_saxpy = 23;
    private static final int RsBlas_dswap = 24;
    private static final int RsBlas_dcopy = 25;
    private static final int RsBlas_daxpy = 26;
    private static final int RsBlas_cswap = 27;
    private static final int RsBlas_ccopy = 28;
    private static final int RsBlas_caxpy = 29;
    private static final int RsBlas_zswap = 30;
    private static final int RsBlas_zcopy = 31;
    private static final int RsBlas_zaxpy = 32;
    private static final int RsBlas_srotg = 33;
    private static final int RsBlas_srotmg = 34;
    private static final int RsBlas_srot = 35;
    private static final int RsBlas_srotm = 36;
    private static final int RsBlas_drotg = 37;
    private static final int RsBlas_drotmg = 38;
    private static final int RsBlas_drot = 39;
    private static final int RsBlas_drotm = 40;
    private static final int RsBlas_sscal = 41;
    private static final int RsBlas_dscal = 42;
    private static final int RsBlas_cscal = 43;
    private static final int RsBlas_zscal = 44;
    private static final int RsBlas_csscal = 45;
    private static final int RsBlas_zdscal = 46;
    private static final int RsBlas_sgemv = 47;
    private static final int RsBlas_sgbmv = 48;
    private static final int RsBlas_strmv = 49;
    private static final int RsBlas_stbmv = 50;
    private static final int RsBlas_stpmv = 51;
    private static final int RsBlas_strsv = 52;
    private static final int RsBlas_stbsv = 53;
    private static final int RsBlas_stpsv = 54;
    private static final int RsBlas_dgemv = 55;
    private static final int RsBlas_dgbmv = 56;
    private static final int RsBlas_dtrmv = 57;
    private static final int RsBlas_dtbmv = 58;
    private static final int RsBlas_dtpmv = 59;
    private static final int RsBlas_dtrsv = 60;
    private static final int RsBlas_dtbsv = 61;
    private static final int RsBlas_dtpsv = 62;
    private static final int RsBlas_cgemv = 63;
    private static final int RsBlas_cgbmv = 64;
    private static final int RsBlas_ctrmv = 65;
    private static final int RsBlas_ctbmv = 66;
    private static final int RsBlas_ctpmv = 67;
    private static final int RsBlas_ctrsv = 68;
    private static final int RsBlas_ctbsv = 69;
    private static final int RsBlas_ctpsv = 70;
    private static final int RsBlas_zgemv = 71;
    private static final int RsBlas_zgbmv = 72;
    private static final int RsBlas_ztrmv = 73;
    private static final int RsBlas_ztbmv = 74;
    private static final int RsBlas_ztpmv = 75;
    private static final int RsBlas_ztrsv = 76;
    private static final int RsBlas_ztbsv = 77;
    private static final int RsBlas_ztpsv = 78;
    private static final int RsBlas_ssymv = 79;
    private static final int RsBlas_ssbmv = 80;
    private static final int RsBlas_sspmv = 81;
    private static final int RsBlas_sger = 82;
    private static final int RsBlas_ssyr = 83;
    private static final int RsBlas_sspr = 84;
    private static final int RsBlas_ssyr2 = 85;
    private static final int RsBlas_sspr2 = 86;
    private static final int RsBlas_dsymv = 87;
    private static final int RsBlas_dsbmv = 88;
    private static final int RsBlas_dspmv = 89;
    private static final int RsBlas_dger = 90;
    private static final int RsBlas_dsyr = 91;
    private static final int RsBlas_dspr = 92;
    private static final int RsBlas_dsyr2 = 93;
    private static final int RsBlas_dspr2 = 94;
    private static final int RsBlas_chemv = 95;
    private static final int RsBlas_chbmv = 96;
    private static final int RsBlas_chpmv = 97;
    private static final int RsBlas_cgeru = 98;
    private static final int RsBlas_cgerc = 99;
    private static final int RsBlas_cher = 100;
    private static final int RsBlas_chpr = 101;
    private static final int RsBlas_cher2 = 102;
    private static final int RsBlas_chpr2 = 103;
    private static final int RsBlas_zhemv = 104;
    private static final int RsBlas_zhbmv = 105;
    private static final int RsBlas_zhpmv = 106;
    private static final int RsBlas_zgeru = 107;
    private static final int RsBlas_zgerc = 108;
    private static final int RsBlas_zher = 109;
    private static final int RsBlas_zhpr = 110;
    private static final int RsBlas_zher2 = 111;
    private static final int RsBlas_zhpr2 = 112;
    private static final int RsBlas_sgemm = 113;
    private static final int RsBlas_ssymm = 114;
    private static final int RsBlas_ssyrk = 115;
    private static final int RsBlas_ssyr2k = 116;
    private static final int RsBlas_strmm = 117;
    private static final int RsBlas_strsm = 118;
    private static final int RsBlas_dgemm = 119;
    private static final int RsBlas_dsymm = 120;
    private static final int RsBlas_dsyrk = 121;
    private static final int RsBlas_dsyr2k = 122;
    private static final int RsBlas_dtrmm = 123;
    private static final int RsBlas_dtrsm = 124;
    private static final int RsBlas_cgemm = 125;
    private static final int RsBlas_csymm = 126;
    private static final int RsBlas_csyrk = 127;
    private static final int RsBlas_csyr2k = 128;
    private static final int RsBlas_ctrmm = 129;
    private static final int RsBlas_ctrsm = 130;
    private static final int RsBlas_zgemm = 131;
    private static final int RsBlas_zsymm = 132;
    private static final int RsBlas_zsyrk = 133;
    private static final int RsBlas_zsyr2k = 134;
    private static final int RsBlas_ztrmm = 135;
    private static final int RsBlas_ztrsm = 136;
    private static final int RsBlas_chemm = 137;
    private static final int RsBlas_cherk = 138;
    private static final int RsBlas_cher2k = 139;
    private static final int RsBlas_zhemm = 140;
    private static final int RsBlas_zherk = 141;
    private static final int RsBlas_zher2k = 142;

    /**
     */
    public static ScriptIntrinsicBLAS create(RenderScript rs) {
        long id = rs.nScriptIntrinsicCreate(13, Element.U32(rs).getID(rs));
        return new ScriptIntrinsicBLAS(id, rs);
    }

    @IntDef({NO_TRANSPOSE, TRANSPOSE, CONJ_TRANSPOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Transpose {}

    @IntDef({UPPER, LOWER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Uplo {}

    @IntDef({NON_UNIT, UNIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Diag {}

    @IntDef({LEFT, RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Side {}

    public static final int NO_TRANSPOSE = 111;
    public static final int TRANSPOSE = 112;
    public static final int CONJ_TRANSPOSE = 113;

    public static final int UPPER = 121;
    public static final int LOWER = 122;

    public static final int NON_UNIT = 131;
    public static final int UNIT = 132;

    public static final int LEFT = 141;
    public static final int RIGHT = 142;

    static void validateSide(@Side int Side) {
        if (Side != LEFT && Side != RIGHT) {
            throw new RSRuntimeException("Invalid side passed to BLAS");
        }
    }

    static void validateTranspose(@Transpose int Trans) {
        if (Trans != NO_TRANSPOSE && Trans != TRANSPOSE &&
            Trans != CONJ_TRANSPOSE) {
            throw new RSRuntimeException("Invalid transpose passed to BLAS");
        }
    }

    static void validateConjTranspose(@Transpose int Trans) {
        if (Trans != NO_TRANSPOSE && 
            Trans != CONJ_TRANSPOSE) {
            throw new RSRuntimeException("Invalid transpose passed to BLAS");
        }
    }

    static void validateDiag(@Diag int Diag) {
        if (Diag != NON_UNIT && Diag != UNIT) {
            throw new RSRuntimeException("Invalid diag passed to BLAS");
        }
    }

    static void validateUplo(@Uplo int Uplo) {
        if (Uplo != LEFT && Uplo != RIGHT) {
            throw new RSRuntimeException("Invalid uplo passed to BLAS");
        }
    }


    /**
     * Level 2 BLAS
     */

    void SGEMV(@Transpose int TransA, float alpha, Allocation A, Allocation X, int incX, Allocation Y, int incY) {
        
    }

    /**
     * Level 3 BLAS
     */

    static void validateL3(Element e, int TransA, int TransB, int Side, Allocation A, Allocation B, Allocation C) {
        int aX = -1, aY = -1, bX = -1, bY = -1, cX = -1, cY = -1;
        if ((A != null && !A.getType().getElement().isCompatible(e)) ||
            (B != null && !B.getType().getElement().isCompatible(e)) ||
            (C != null && !C.getType().getElement().isCompatible(e))) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        if (C != null) {
            cX = C.getType().getY();
            cY = C.getType().getX();
        }
        if (Side == RIGHT) {
            if (B != null) {
                bX = A.getType().getY();
                bY = A.getType().getX();
            }
            if (A != null) {
                aX = B.getType().getY();
                aY = B.getType().getX();
            }
        } else {
            if (A != null) {
                if (TransA == TRANSPOSE) {
                    aY = A.getType().getY();
                    aX = A.getType().getX();
                } else {
                    aX = A.getType().getY();
                    aY = A.getType().getX();
                }
            }
            if (B != null) {
                if (TransB == TRANSPOSE) {
                    bY = B.getType().getY();
                    bX = B.getType().getX();
                } else {
                    bX = B.getType().getY();
                    bY = B.getType().getX();
                }
            }
        }
        if (A != null && B != null && C != null) {
            if (aY != bX || aX != cX || bY != cY) {
                throw new RSRuntimeException("Called BLAS with invalid dimensions");
            }
        } else if (A != null && C != null) {
            // A and C only
            if (aX != cY || aY != cX) {
                throw new RSRuntimeException("Called BLAS with invalid dimensions");
            }
        } else if (A != null && B != null) {
            // A and B only
        }

    }

    public void SGEMM(@Transpose int TransA, @Transpose int TransB, float alpha, Allocation A,
                      Allocation B, float beta, Allocation C) {
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F32(mRS), TransA, TransB, 0, A, B, C);

        int M = -1, N = -1, K = -1;
        if (TransA == TRANSPOSE) {
            M = A.getType().getX();
            K = A.getType().getY();           
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        if (TransB == TRANSPOSE) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX(); 
        }
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_sgemm, TransA, TransB, 0, 0, 0, M, N, K,  alpha, A.getID(mRS), B.getID(mRS),
                                        beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void DGEMM(@Transpose int TransA, @Transpose int TransB, double alpha, Allocation A,
                      Allocation B, double beta, Allocation C) {
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F64(mRS), TransA, TransB, 0, A, B, C);
        int M = -1, N = -1, K = -1;
        if (TransA == TRANSPOSE) {
            M = A.getType().getX();
            K = A.getType().getY();           
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        if (TransB == TRANSPOSE) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX(); 
        }
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_dgemm, TransA, TransB, 0, 0, 0, M, N, K,  alpha, A.getID(mRS), B.getID(mRS),
                                        beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void CGEMM(@Transpose int TransA, @Transpose int TransB, Float2 alpha, Allocation A,
                      Allocation B, Float2 beta, Allocation C) {
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F32_2(mRS), TransA, TransB, 0, A, B, C);
        int M = -1, N = -1, K = -1;
        if (TransA == TRANSPOSE) {
            M = A.getType().getX();
            K = A.getType().getY();           
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        if (TransB == TRANSPOSE) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX(); 
        }
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_cgemm, TransA, TransB, 0, 0, 0, M, N, K,  alpha.x, alpha.y, A.getID(mRS), B.getID(mRS),
                                         beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }

    public void ZGEMM(@Transpose int TransA, @Transpose int TransB, Double2 alpha, Allocation A,
                      Allocation B, Double2 beta, Allocation C) {
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F64_2(mRS), TransA, TransB, 0, A, B, C);
        int M = -1, N = -1, K = -1;
        if (TransA == TRANSPOSE) {
            M = A.getType().getX();
            K = A.getType().getY();           
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        if (TransB == TRANSPOSE) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX(); 
        }
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zgemm, TransA, TransB, 0, 0, 0, M, N, K,  alpha.x, alpha.y, A.getID(mRS), B.getID(mRS),
                                   beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }

    public void SSYMM(@Side int Side, @Uplo int Uplo, float alpha, Allocation A,
                      Allocation B, float beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        validateL3(Element.F32(mRS), 0, 0, Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_ssymm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0, alpha, A.getID(mRS), B.getID(mRS),
                                        beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void DSYMM(@Side int Side, @Uplo int Uplo, double alpha, Allocation A,
                      Allocation B, double beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        validateL3(Element.F64(mRS), 0, 0, Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_dsymm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0, alpha, A.getID(mRS), B.getID(mRS),
                                        beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void CSYMM(@Side int Side, @Uplo int Uplo, Float2 alpha, Allocation A,
                      Allocation B, Float2 beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        validateL3(Element.F32_2(mRS), 0, 0, Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_csymm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0, alpha.x, alpha.y, A.getID(mRS), B.getID(mRS),
                                         beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZSYMM(@Side int Side, @Uplo int Uplo, Double2 alpha, Allocation A,
                      Allocation B, Double2 beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        validateL3(Element.F64_2(mRS), 0, 0, Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zsymm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0, alpha.x, alpha.y, A.getID(mRS), B.getID(mRS),
                                   beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }

    public void SSYRK(@Uplo int Uplo, @Transpose int Trans, float alpha, Allocation A, float beta, Allocation C) {
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F32(mRS), Trans, 0, 0, A, null, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_ssyrk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(mRS), 0, beta, C.getID(mRS), 0, 0, 0, 0);
    }

    public void DSYRK(@Uplo int Uplo, @Transpose int Trans, double alpha, Allocation A, double beta, Allocation C) {
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F64(mRS), Trans, 0, 0, A, null, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_dsyrk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(mRS), 0, beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void CSYRK(@Uplo int Uplo, @Transpose int Trans, float alphaX, float alphaY, Allocation A, float betaX, float betaY, Allocation C) {
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F32_2(mRS), Trans, 0, 0, A, null, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_csyrk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alphaX, alphaY, A.getID(mRS), 0, betaX, betaY,
                                        C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZSYRK(@Uplo int Uplo, @Transpose int Trans, double alphaX, double alphaY, Allocation A, double betaX, double betaY, Allocation C) {
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F64_2(mRS), Trans, 0, 0, A, null, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zsyrk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alphaX, alphaY, A.getID(mRS), 0, betaX, betaY,
                                   C.getID(mRS), 0, 0, 0, 0);
    }

    static void validateSYR2K(Element e, @Transpose int Trans, Allocation A, Allocation B, Allocation C) {
        validateTranspose(Trans);
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        int Cdim = -1;
        // A is n x k if no transpose, k x n if transpose
        // C is n x n
        if (Trans == TRANSPOSE) {
            // check columns versus C
            Cdim = A.getType().getX();
        } else {
            // check rows versus C
            Cdim = A.getType().getY();
        }
        if (C.getType().getX() != Cdim && C.getType().getY() != Cdim) {
            throw new RSRuntimeException("Invalid symmetric matrix in SYR2K");
        }
        // A dims == B dims
        if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
            throw new RSRuntimeException("Invalid A and B in SYR2K");
        }
    }
    public void SSYR2K(@Uplo int Uplo, @Transpose int Trans, float alpha, Allocation A, Allocation B, float beta, Allocation C) {
        validateUplo(Uplo);
        validateSYR2K(Element.F32(mRS), Trans, A, B, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();            
        }
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_ssyr2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(mRS), B.getID(mRS), beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void DSYR2K(@Uplo int Uplo, @Transpose int Trans, double alpha, Allocation A, Allocation B, double beta, Allocation C) {
        validateUplo(Uplo);
        validateSYR2K(Element.F64(mRS), Trans, A, B, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();            
        }
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_ssyr2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(mRS), B.getID(mRS), beta, C.getID(mRS), 0, 0, 0, 0);
    }
    public void CSYR2K(@Uplo int Uplo, @Transpose int Trans, Float2 alpha, Allocation A, Allocation B, Float2 beta, Allocation C) {
        validateUplo(Uplo);
        validateSYR2K(Element.F32_2(mRS), Trans, A, B, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();            
        }
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_ssyr2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZSYR2K(@Uplo int Uplo, @Transpose int Trans, Double2 alpha, Allocation A, Allocation B, Double2 beta, Allocation C) {
        validateUplo(Uplo);
        validateSYR2K(Element.F64_2(mRS), Trans, A, B, C);
        int K = -1;
        if (Trans == TRANSPOSE) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();            
        }
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_ssyr2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), beta.x, beta.y, C.getID(mRS), 0, 0, 0, 0);
    }

    static void validateTRMM(Element e, @Side int Side, @Transpose int TransA, Allocation A, Allocation B) {
        validateSide(Side);
        validateTranspose(TransA);
        int aX = -1, aY = -1, bX = -1, bY = -1;
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        if (TransA == TRANSPOSE) {
            aY = A.getType().getY();
            aX = A.getType().getX();
        } else {
            aY = A.getType().getX();
            aX = A.getType().getY();
        }
        bX = B.getType().getY();
        bY = B.getType().getX();
        if (Side == LEFT) {
            if (aX == 0 || aY != bX) {
                throw new RSRuntimeException("Called TRMM with invalid matrices");
            }
        } else {
            if (bY != aX || aY == 0) {
                throw new RSRuntimeException("Called TRMM with invalid matrices");
            }
        }
    }
    public void STRMM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, float alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRMM(Element.F32(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_strmm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                        alpha, A.getID(mRS), B.getID(mRS), 0.f, 0, 0, 0, 0, 0);
    }
    public void DTRMM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, double alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRMM(Element.F64(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_strmm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                        alpha, A.getID(mRS), B.getID(mRS), 0.f, 0, 0, 0, 0, 0);
    }
    public void CTRMM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, Float2 alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRMM(Element.F32_2(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_strmm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                         alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0, 0);
    }
    public void ZTRMM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, Double2 alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRMM(Element.F64_2(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_strmm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                   alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0, 0);
    }

    static void validateTRSM(Element e, @Side int Side, @Transpose int TransA, Allocation A, Allocation B) {
        int adim = -1, bX = -1, bY = -1;
        validateSide(Side);
        validateTranspose(TransA);
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        adim = A.getType().getX();
        if (adim != A.getType().getY()) {
            // this may be unnecessary, the restriction could potentially be relaxed
            // A needs to contain at least that symmetric matrix but could theoretically be larger
            // for now we assume adapters are sufficient, will reevaluate in the future
            throw new RSRuntimeException("Called TRSM with a non-symmetric matrix A");
        }
        bX = B.getType().getY();
        bY = B.getType().getX();
        if (Side == LEFT) {
            // A is M*M
            if (adim != bY) {
                throw new RSRuntimeException("Called TRSM with invalid matrix dimensions");
            }
        } else {
            // A is N*N
            if (adim != bX) {
                throw new RSRuntimeException("Called TRSM with invalid matrix dimensions");
            }
        }
    }
    public void STRSM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, float alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRSM(Element.F32(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Single(getID(mRS), RsBlas_strsm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                        alpha, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0);
    }
    public void DTRSM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, double alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRSM(Element.F64(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Double(getID(mRS), RsBlas_strsm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                        alpha, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0);
    }
    public void CTRSM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, Float2 alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRSM(Element.F32_2(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_strsm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                         alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0, 0);
    }
    public void ZTRSM(@Side int Side, @Uplo int Uplo, @Transpose int TransA, @Diag int Diag, Double2 alpha, Allocation A, Allocation B) {
        validateUplo(Uplo);
        validateDiag(Diag);
        validateTRSM(Element.F64_2(mRS), Side, TransA, A, B);
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_strsm, TransA, 0, Side, Uplo, Diag, B.getType().getY(), B.getType().getX(), 0,
                                   alpha.x, alpha.y, A.getID(mRS), B.getID(mRS), 0, 0, 0, 0, 0, 0, 0);
    }

    static void validateHEMM(Element e, @Side int Side, Allocation A, Allocation B, Allocation C) {
        validateSide(Side);

        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }

        // A must be square; can potentially be relaxed similar to TRSM
        int adim = A.getType().getX();
        if (adim != A.getType().getY()) {
            throw new RSRuntimeException("Called HEMM with non-square A");
        }
        if ((Side == LEFT && adim != B.getType().getY()) ||
            (Side == RIGHT && adim != B.getType().getX())) {
            throw new RSRuntimeException("Called HEMM with invalid B");
        }
        if (B.getType().getX() != C.getType().getX() || 
            B.getType().getY() != C.getType().getY()) {
            throw new RSRuntimeException("Called HEMM with mismatched B and C");
        }
    }
    public void CHEMM(@Side int Side, @Uplo int Uplo, float alpha, Allocation A, Allocation B, float beta, Allocation C) {
        validateUplo(Uplo);
        validateHEMM(Element.F32_2(mRS), Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_chemm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0,
                                         alpha, 0, A.getID(mRS), B.getID(mRS), beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZHEMM(@Side int Side, @Uplo int Uplo, double alpha, Allocation A, Allocation B, double beta, Allocation C) {
        validateUplo(Uplo);
        validateHEMM(Element.F32_2(mRS), Side, A, B, C);
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zhemm, 0, 0, Side, Uplo, 0, C.getType().getY(), C.getType().getX(), 0,
                                   alpha, 0, A.getID(mRS), B.getID(mRS), beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }

    static void validateHERK(Element e, @Transpose int Trans, Allocation A, Allocation C) {
        if (!A.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        validateConjTranspose(Trans);
        int cdim = C.getType().getX();
        if (cdim != C.getType().getY()) {
            throw new RSRuntimeException("Called HERK with non-square C");
        }
        if (Trans == NO_TRANSPOSE) {
            if (cdim != A.getType().getX()) {
                throw new RSRuntimeException("Called HERK with invalid A");
            }
        } else {
            if (cdim != A.getType().getY()) {
                throw new RSRuntimeException("Called HERK with invalid A");
            }
        }
    }
    public void CHERK(@Uplo int Uplo, @Transpose int Trans, float alpha, Allocation A, float beta, Allocation C) {
        validateUplo(Uplo);
        validateHERK(Element.F32_2(mRS), Trans, A, C);
        int k = 0;
        if (Trans == TRANSPOSE) {
            k = A.getType().getY();
        } else {
            k = A.getType().getX();
        }
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_cherk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), k,
                                         alpha, 0, A.getID(mRS), 0, beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZHERK(@Uplo int Uplo, @Transpose int Trans, double alpha, Allocation A, double beta, Allocation C) {
        validateUplo(Uplo);
        validateHERK(Element.F64_2(mRS), Trans, A, C);
        int k = 0;
        if (Trans == TRANSPOSE) {
            k = A.getType().getY();
        } else {
            k = A.getType().getX();
        }
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zherk, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), k,
                                   alpha, 0, A.getID(mRS), 0, beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }

    static void validateHER2K(Element e, @Transpose int Trans, Allocation A, Allocation B, Allocation C) {
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        }
        validateConjTranspose(Trans);
        int cdim = C.getType().getX();
        if (cdim != C.getType().getY()) {
            throw new RSRuntimeException("Called HER2K with non-square C");
        }
        if (Trans == NO_TRANSPOSE) {
            if (A.getType().getY() != cdim) {
                throw new RSRuntimeException("Called HER2K with invalid matrices");
            }
        } else {
            if (A.getType().getX() != cdim) {
                throw new RSRuntimeException("Called HER2K with invalid matrices");
            }
        }
        if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
            throw new RSRuntimeException("Called HER2K with invalid A and B matrices");
        }
    }
    public void CHER2K(@Uplo int Uplo, @Transpose int Trans, Float2 alpha, Allocation A, Allocation B, float beta, Allocation C) {
        validateUplo(Uplo);
        validateHER2K(Element.F32_2(mRS), Trans, A, B, C);
        int k = 0;
        if (Trans == NO_TRANSPOSE) {
            k = A.getType().getX();
        } else {
            k = A.getType().getY();
        }
        mRS.nScriptIntrinsicBLAS_Complex(getID(mRS), RsBlas_cher2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), k, alpha.x, alpha.y,
                                         A.getID(mRS), B.getID(mRS), beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }
    public void ZHER2K(@Uplo int Uplo, @Transpose int Trans, Double2 alpha, Allocation A, Allocation B, double beta, Allocation C) {
        validateUplo(Uplo);
        validateHER2K(Element.F64_2(mRS), Trans, A, B, C);
        int k = 0;
        if (Trans == NO_TRANSPOSE) {
            k = A.getType().getX();
        } else {
            k = A.getType().getY();
        }
        mRS.nScriptIntrinsicBLAS_Z(getID(mRS), RsBlas_zher2k, Trans, 0, 0, Uplo, 0, 0, C.getType().getX(), k, alpha.x, alpha.y,
                                   A.getID(mRS), B.getID(mRS), beta, 0, C.getID(mRS), 0, 0, 0, 0);
    }



}
