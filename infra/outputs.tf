output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer — point your domain's CNAME here"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL — used in CI/CD to push and tag images"
  value       = aws_ecr_repository.app.repository_url
}

output "rds_proxy_endpoint" {
  description = "RDS Proxy endpoint — used as SPRING_DATASOURCE_URL host"
  value       = aws_db_proxy.main.endpoint
}

output "redis_primary_endpoint" {
  description = "ElastiCache Redis primary endpoint — used as REDIS_URL host"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "ecs_cluster_name" {
  description = "ECS cluster name — used in CI/CD deploy step"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name — used in CI/CD rolling deploy"
  value       = aws_ecs_service.app.name
}

output "cloudwatch_dashboard_url" {
  description = "Direct link to the CloudWatch dashboard"
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.main.dashboard_name}"
}
