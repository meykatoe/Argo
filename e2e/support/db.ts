import pg from 'pg'

const url = process.env.E2E_DB_URL ?? 'postgres://argo:1234@127.0.0.1:5432/argo'
const pool = new pg.Pool({ connectionString: url, max: 2 })

export interface Seed {
  setId: string
  cardId: number
  cardName: string
  email: string
}

// 每次執行用獨立編號，不碰既有資料
export async function seedCard(): Promise<Seed> {
  const run = Date.now().toString(36).toUpperCase()
  const setId = `E2E${run}`
  const cardName = `E2E 測試卡 ${run}`
  await pool.query(
    `insert into card_set (set_id, set_name, category, on_sale) values ($1, $2, 'promo', 1)`,
    [setId, `E2E ${run}`],
  )
  const res = await pool.query(
    `insert into card (card_set_id, card_image_id, set_id, card_name, rarity, card_color, card_type,
                       market_price, sale_price, stock, source_key)
     values ($1, $1, $2, $3, 'C', 'Red', 'Character', 5.56, 5.00, 3, $4) returning id`,
    [`${setId}-001`, setId, cardName, `e2e:${setId}`],
  )
  return { setId, cardId: Number(res.rows[0].id), cardName, email: `e2e-${run.toLowerCase()}@example.test` }
}

export async function orderStatus(email: string): Promise<string | undefined> {
  const res = await pool.query(`select status from shop_order where customer_email = $1`, [email])
  return res.rows[0]?.status
}

export async function cardStock(cardId: number): Promise<number> {
  const res = await pool.query(`select stock from card where id = $1`, [cardId])
  return res.rows[0].stock
}

export async function cleanup(seed: Seed): Promise<void> {
  const orders = `select id from shop_order where customer_email = $1`
  await pool.query(`delete from payment where order_id in (${orders})`, [seed.email])
  await pool.query(`delete from shop_order_item where order_id in (${orders})`, [seed.email])
  await pool.query(`delete from shop_order where customer_email = $1`, [seed.email])
  await pool.query(`delete from card where id = $1`, [seed.cardId])
  await pool.query(`delete from card_set where set_id = $1`, [seed.setId])
}

export async function closeDb(): Promise<void> {
  await pool.end()
}
