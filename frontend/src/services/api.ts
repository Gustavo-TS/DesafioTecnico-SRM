export type Moeda = 'BRL' | 'USD'
export type TipoRecebivel = 'DUPLICATA' | 'CHEQUE'
export type StatusRecebivel = 'PENDENTE' | 'LIQUIDADO'

export type Cedente = { id: string; nome: string; documento: string; criadoEm: string }
export type Recebivel = { id: string; cedenteId: string; tipo: TipoRecebivel; valorFace: number; dataVencimento: string; status: StatusRecebivel; criadoEm: string }
export type TaxaCambio = { id: string; moedaOrigem: Moeda; moedaDestino: Moeda; taxa: number; vigenteEm: string; criadoEm: string }
export type Resultado = { valorFace: number; taxaBase: number; spread: number; prazoMeses: number; valorPresente: number; valorDesagio: number; moedaPagamento: Moeda; taxaCambioUtilizada?: number | null; valorFinal: number }
export type Liquidacao = Resultado & { id: string; recebivelId: string; taxaCambioVigenteEm?: string | null; liquidadoEm: string }

export type CriarCedente = Pick<Cedente, 'nome' | 'documento'>
export type CriarRecebivel = Pick<Recebivel, 'cedenteId' | 'tipo' | 'valorFace' | 'dataVencimento'>
export type CriarTaxaCambio = Pick<TaxaCambio, 'moedaOrigem' | 'moedaDestino' | 'taxa' | 'vigenteEm'>
export type SimularPrecificacao = Pick<Recebivel, 'tipo' | 'valorFace' | 'dataVencimento'> & { moedaPagamento: Moeda }
export type FiltrosLiquidacao = { dataInicio?: string; dataFim?: string; cedenteId?: string; moeda?: Moeda }

const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

const nomesCampos: Record<string, string> = {
  nome: 'Nome',
  documento: 'Documento',
  cedenteId: 'Cedente',
  recebivelId: 'Recebível',
  tipo: 'Tipo',
  valorFace: 'Valor de face',
  dataVencimento: 'Data de vencimento',
  moedaPagamento: 'Moeda de pagamento',
  moedaOrigem: 'Moeda de origem',
  moedaDestino: 'Moeda de destino',
  taxa: 'Taxa',
  vigenteEm: 'Vigência',
  dataInicio: 'Data inicial',
  dataFim: 'Data final',
}

async function mensagemErro(response: Response) {
  const text = await response.text()
  if (!text) return `Erro na requisição (${response.status}).`

  try {
    const body = JSON.parse(text) as { erro?: unknown; message?: unknown; campos?: Record<string, unknown> }
    const mensagem = typeof body.erro === 'string' && body.erro
      ? body.erro
      : typeof body.message === 'string' && body.message
        ? body.message
        : ''
    const detalhes = Object.entries(body.campos ?? {})
      .filter(([, erro]) => typeof erro === 'string' && erro)
      .map(([campo, erro]) => `${nomesCampos[campo] ?? campo}: ${erro}`)

    if (mensagem || detalhes.length) return [mensagem, ...detalhes].filter(Boolean).join('\n')
  } catch {}

  return text
}

async function requisitar<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(baseUrl + path, { ...options, headers })
  } catch {
    throw new Error('Não foi possível conectar à API. Verifique se o backend está em execução.')
  }

  if (!response.ok) throw new Error(await mensagemErro(response))

  const text = await response.text()
  return text ? JSON.parse(text) as T : undefined as T
}

export const api = {
  listarCedentes: () => requisitar<Cedente[]>('/api/cedentes'),
  criarCedente: (body: CriarCedente) => requisitar<Cedente>('/api/cedentes', { method: 'POST', body: JSON.stringify(body) }),

  listarRecebiveis: () => requisitar<Recebivel[]>('/api/recebiveis'),
  buscarRecebivel: (id: string) => requisitar<Recebivel>(`/api/recebiveis/${id}`),
  criarRecebivel: (body: CriarRecebivel) => requisitar<Recebivel>('/api/recebiveis', { method: 'POST', body: JSON.stringify(body) }),

  simular: (body: SimularPrecificacao) => requisitar<Resultado>('/api/precificacoes/simular', { method: 'POST', body: JSON.stringify(body) }),

  criarTaxaCambio: (body: CriarTaxaCambio) => requisitar<TaxaCambio>('/api/taxas-cambio', { method: 'POST', body: JSON.stringify(body) }),
  buscarTaxaVigente: (moedaOrigem: Moeda, moedaDestino: Moeda) => requisitar<TaxaCambio>(`/api/taxas-cambio/vigente?${new URLSearchParams({ moedaOrigem, moedaDestino })}`),

  liquidar: (recebivelId: string, moedaPagamento: Moeda, idempotencyKey: string) => requisitar<Liquidacao>(`/api/recebiveis/${recebivelId}/liquidacoes`, { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify({ moedaPagamento }) }),
  listarLiquidacoes: (filtros: FiltrosLiquidacao = {}) => requisitar<Liquidacao[]>(`/api/liquidacoes?${new URLSearchParams(Object.entries(filtros).filter(([, value]) => value) as [string, string][])}`),
}
