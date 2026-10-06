import { ref } from 'vue'

// 後端回報這個 IP 被封鎖時為真，整個網站改顯示封鎖畫面
export const blocked = ref(false)
