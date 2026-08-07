export type MembershipPackage = { id: number; name: string; durationDays: number; price: number; description: string };
export type AdminMembershipPackage = MembershipPackage & { isActive: boolean; createdAt?: string; updatedAt?: string };
export type MembershipPackageInput = { name: string; durationDays: number; price: number; description: string };
export type PackageErrorCode = "SUB-002" | "SUB-007" | "VAL-001" | "ACC-004" | "ACC-005" | "ACC-006" | "AUTH-002" | "NETWORK-001" | "SYS-001";
export type PackageSuccess<T> = { success: true; message: string; data: T };
export type PackageErrorResponse = { success: false; errorCode: PackageErrorCode; message: string; details?: Record<string, unknown> };
