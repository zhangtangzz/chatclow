import request from './request'

// 获取启用的模型列表
export function getModelList() {
  return request.get('/model/list')
}

// 测试模型连接
export function testModelConnection(data) {
  return request.post('/model/test', data)
}
