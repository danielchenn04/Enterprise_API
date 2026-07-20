variable "app_name" {
  description = "Application name, used as a prefix for all resources"
  type        = string
  default     = "enterprise-api"
}

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (prod, staging)"
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "enterpriseapi"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "app"
}

variable "db_password" {
  description = "PostgreSQL master password (store in tfvars, never commit)"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Base64-encoded 256-bit JWT signing secret"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "Full ECR image URI with tag (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/enterprise-api:latest)"
  type        = string
  default     = "placeholder"  # overridden by CI/CD pipeline
}

variable "task_cpu" {
  description = "ECS task CPU units (256, 512, 1024, 2048, 4096)"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "ECS task memory in MiB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Desired number of ECS task replicas"
  type        = number
  default     = 2
}

variable "waf_rate_limit" {
  description = "Max requests per IP per 5-minute window before WAF blocks"
  type        = number
  default     = 2000
}
