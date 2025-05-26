{{/*
Return the job-manage Service enabled
判断条件：
1.job-manage模块开启
2.标准模式才部署Service
3.全链路灰度环境下，基础泳道才部署Service
*/}}
{{- define "job.manage.service.enabled" -}}
{{- if (not .Values.manageConfig.enabled) -}}
{{- false -}}
{{- else if (ne .Values.deploy.mode "standard") -}}
{{- false -}}
{{- else if (ne .Values.laneType "base") -}}
{{- false -}}
{{- else -}}
{{- true -}}
{{- end -}}
{{- end -}}
