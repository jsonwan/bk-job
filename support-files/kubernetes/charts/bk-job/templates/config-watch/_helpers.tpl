{{/*
Return the config-watch Service enabled
判断条件：
1.config-watch模块开启
2.全链路灰度环境下，基础泳道才部署Service
*/}}
{{- define "job.configWatch.service.enabled" -}}
{{- if (not .Values.k8sConfigWatcherConfig.enabled) -}}
{{- false -}}
{{- else if (ne .Values.laneType "base") -}}
{{- false -}}
{{- else -}}
{{- true -}}
{{- end -}}
{{- end -}}
