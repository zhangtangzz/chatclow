import request from './request'

// 获取启用的模型列表
export function getModelList() {
  return request.get('/model/list')
}
