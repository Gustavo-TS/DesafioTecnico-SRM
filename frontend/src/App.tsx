import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, type Cedente, type Liquidacao, type Moeda, type Recebivel, type Resultado, type TaxaCambio, type TipoRecebivel } from './services/api'
import logoSrm from './assets/srm-logo.png'
import './styles.css'

const moedas: Moeda[] = ['BRL', 'USD']
const tipos: TipoRecebivel[] = ['DUPLICATA', 'CHEQUE']
const dinheiro = (valor: number | undefined, moeda: Moeda = 'BRL') => valor == null ? '—' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda }).format(valor)
const percentual = (valor: number) => new Intl.NumberFormat('pt-BR', { style: 'percent', minimumFractionDigits: 2 }).format(valor)
type Secao = 'todos' | 'simulacao' | 'cedente' | 'recebivel' | 'cambio' | 'liquidacao' | 'extrato'

function App() {
  const [cedentes, setCedentes] = useState<Cedente[]>([])
  const [recebiveis, setRecebiveis] = useState<Recebivel[]>([])
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [resultadoLiquidacao, setResultadoLiquidacao] = useState<Liquidacao | null>(null)
  const [liquidacoes, setLiquidacoes] = useState<Liquidacao[]>([])
  const [taxaVigente, setTaxaVigente] = useState<TaxaCambio | null>(null)
  const [secao, setSecao] = useState<Secao>('todos')
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')
  const [busy, setBusy] = useState('')
  const operacaoLiquidacao = useRef<{
    chave: string
    recebivelId: string
    moedaPagamento: Moeda
  } | null>(null)
  const formularioSimulacao = useRef<HTMLFormElement | null>(null)
  const [versaoSimulacao, setVersaoSimulacao] = useState(0)
  const sequenciaSimulacao = useRef(0)
  const simulacaoAutomaticaHabilitada = useRef(false)

  const carregar = async () => {
    const [listaCedentes, listaRecebiveis] = await Promise.all([api.listarCedentes(), api.listarRecebiveis()])
    setCedentes(listaCedentes)
    setRecebiveis(listaRecebiveis)
  }

  const mostrarErro = (error: unknown) => setErro(error instanceof Error ? error.message : 'Erro ao processar solicitação.')
  const executarSimulacao = async (form: HTMLFormElement) => {
    const d = new FormData(form)
    const valorFace = Number(d.get('valorFace'))
    const dataVencimento = String(d.get('dataVencimento') || '')
    const tipo = String(d.get('tipo')) as TipoRecebivel
    const moedaPagamento = String(d.get('moedaPagamento')) as Moeda
    const sequenciaAtual = ++sequenciaSimulacao.current

    try {
      const resposta = await api.simular({
        valorFace,
        dataVencimento,
        tipo,
        moedaPagamento,
      })

      if (sequenciaAtual === sequenciaSimulacao.current) {
        setResultado(resposta)
      }
    } catch (error) {
      if (sequenciaAtual === sequenciaSimulacao.current) {
        mostrarErro(error)
      }
    }
  }
  useEffect(() => { void carregar().catch(mostrarErro) }, [])
  useEffect(() => {
    if (!simulacaoAutomaticaHabilitada.current || versaoSimulacao === 0) return

    setResultado(null)
    const timer = window.setTimeout(async () => {
      const form = formularioSimulacao.current
      if (!form) return

      const d = new FormData(form)
      const valorFace = Number(d.get('valorFace'))
      const dataVencimento = String(d.get('dataVencimento') || '')

      if (!valorFace || valorFace <= 0 || !dataVencimento) return

      const hoje = new Date()
      hoje.setHours(0, 0, 0, 0)

      const vencimento = new Date(`${dataVencimento}T00:00:00`)
      if (vencimento <= hoje) return

      await executarSimulacao(form)
    }, 400)

    return () => window.clearTimeout(timer)
  }, [versaoSimulacao])
  const enviar = (nome: string, acao: (form: HTMLFormElement) => Promise<void>) => async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault(); setBusy(nome); setMensagem(''); setErro('')
    try { await acao(evento.currentTarget) } catch (erro) { mostrarErro(erro) } finally { setBusy('') }
  }
  const campo = (name: string, label: string, type = 'text', obrigatorio = true) => <label>{label}<input required={obrigatorio} name={name} type={type} step={type === 'number' ? 'any' : undefined} min={type === 'date' ? '1000-01-01' : type === 'datetime-local' ? '1000-01-01T00:00' : undefined} max={type === 'date' ? '9999-12-31' : type === 'datetime-local' ? '9999-12-31T23:59' : undefined} /></label>
  const opcoesMoeda = moedas.map(moeda => <option key={moeda}>{moeda}</option>)
  const opcoesTipo = tipos.map(tipo => <option key={tipo}>{tipo}</option>)
  const recebiveisPendentes = recebiveis.filter(recebivel => recebivel.status === 'PENDENTE')
  const nomeCedente = (recebivelId: string) => {
    const recebivel = recebiveis.find(item => item.id === recebivelId)
    return cedentes.find(item => item.id === recebivel?.cedenteId)?.nome ?? '—'
  }

  return <main>
    <header className="topbar"><div className="brand"><img src={logoSrm} alt="SRM Capital em movimento" /></div><div className="header-copy"><span>Operações de crédito</span><h1>Credit Engine</h1></div><nav aria-label="Seções do painel">{([{ id: 'todos', label: 'Todos' }, { id: 'simulacao', label: 'Simulação' }, { id: 'cedente', label: 'Cedente' }, { id: 'recebivel', label: 'Recebível' }, { id: 'cambio', label: 'Câmbio' }, { id: 'liquidacao', label: 'Liquidação' }, { id: 'extrato', label: 'Extrato' }] as const).map(item => <button type="button" key={item.id} className={secao === item.id ? 'active' : ''} onClick={() => setSecao(item.id)}>{item.label}</button>)}</nav></header>
    {mensagem && <div className="alert">{mensagem}</div>}
    {erro && <div className="modal-backdrop" role="presentation" onClick={() => setErro('')}><div className="error-modal" role="alertdialog" aria-modal="true" aria-label="Mensagem de erro" onClick={event => event.stopPropagation()}><button className="modal-close" type="button" aria-label="Fechar mensagem de erro" onClick={() => setErro('')}>×</button><p>{erro}</p></div></div>}

    <section id="simulacao" className="principal" hidden={secao !== 'todos' && secao !== 'simulacao'}><h2>Simulação</h2><form ref={formularioSimulacao} onChange={() => { sequenciaSimulacao.current++; setVersaoSimulacao(valor => valor + 1) }} onSubmit={evento => { simulacaoAutomaticaHabilitada.current = true; void enviar('simular', executarSimulacao)(evento) }}>{campo('valorFace', 'Valor de face', 'number')}{campo('dataVencimento', 'Data de vencimento', 'date')}<label>Tipo<select name="tipo">{opcoesTipo}</select></label><label>Moeda de pagamento<select name="moedaPagamento">{opcoesMoeda}</select></label><button disabled={busy === 'simular'}>Simular</button></form>{resultado && <div className="resultado resumo-simulacao"><b>Resumo da simulação</b><p>Valor presente <strong>{dinheiro(resultado.valorPresente, 'BRL')}</strong></p><p>Deságio <strong>{dinheiro(resultado.valorDesagio, 'BRL')}</strong></p><p>Prazo <strong>{resultado.prazoMeses} meses</strong></p><p>Taxa base <strong>{percentual(resultado.taxaBase)}</strong></p><p>Spread <strong>{percentual(resultado.spread)}</strong></p>{resultado.taxaCambioUtilizada != null && <p>Taxa de câmbio <strong>{resultado.taxaCambioUtilizada}</strong></p>}<p className="total">Valor final <strong>{dinheiro(resultado.valorFinal, resultado.moedaPagamento)}</strong></p></div>}</section>

    <div className="cols" hidden={secao !== 'todos' && secao !== 'cedente' && secao !== 'cambio'}><section hidden={secao !== 'todos' && secao !== 'cedente'}><h2>Cadastro de cedente</h2><form onSubmit={enviar('cedente', async form => {
      const d = new FormData(form); await api.criarCedente({ nome: String(d.get('nome')), documento: String(d.get('documento')) }); await carregar(); setMensagem('Cedente cadastrado.')
    })}>{campo('nome', 'Nome')}{campo('documento', 'Documento')}<button disabled={busy === 'cedente'}>Cadastrar cedente</button></form></section>
      <section hidden={secao !== 'todos' && secao !== 'cambio'}><h2>Cadastro de taxa de câmbio</h2><form onSubmit={enviar('cambio', async form => {
        const d = new FormData(form); await api.criarTaxaCambio({ moedaOrigem: String(d.get('origem')) as Moeda, moedaDestino: String(d.get('destino')) as Moeda, taxa: Number(d.get('taxa')), vigenteEm: new Date(String(d.get('vigenteEm'))).toISOString() }); setMensagem('Taxa de câmbio cadastrada.')
      })}><label>Origem<select name="origem">{opcoesMoeda}</select></label><label>Destino<select name="destino">{opcoesMoeda}</select></label>{campo('taxa', 'Taxa', 'number')}{campo('vigenteEm', 'Vigente em', 'datetime-local')}<button disabled={busy === 'cambio'}>Cadastrar taxa</button></form><div className="consulta-cambio" hidden={secao !== 'cambio'}><h2>Consultar taxa vigente</h2><form onSubmit={enviar('consultar-cambio', async form => {
        const d = new FormData(form); setTaxaVigente(await api.buscarTaxaVigente(String(d.get('origem')) as Moeda, String(d.get('destino')) as Moeda))
      })}><label>Origem<select name="origem">{opcoesMoeda}</select></label><label>Destino<select name="destino">{opcoesMoeda}</select></label><button disabled={busy === 'consultar-cambio'}>Consultar taxa</button></form>{taxaVigente && <div className="resultado"><b>Taxa vigente</b><p>Par <strong>{taxaVigente.moedaOrigem} → {taxaVigente.moedaDestino}</strong></p><p className="total">Taxa <strong>{taxaVigente.taxa}</strong></p></div>}</div></section></div>

    <section hidden={secao !== 'todos' && secao !== 'recebivel'}><h2>Cadastro de recebível</h2><form onSubmit={enviar('recebivel', async form => {
      const d = new FormData(form); await api.criarRecebivel({ cedenteId: String(d.get('cedenteId')), tipo: String(d.get('tipo')) as TipoRecebivel, valorFace: Number(d.get('valorFace')), dataVencimento: String(d.get('dataVencimento')) }); await carregar(); setMensagem('Recebível cadastrado.')
    })}><label>Cedente<select required name="cedenteId"><option value="">Selecione</option>{cedentes.map(cedente => <option key={cedente.id} value={cedente.id}>{cedente.nome}</option>)}</select></label><label>Tipo<select name="tipo">{opcoesTipo}</select></label>{campo('valorFace', 'Valor de face', 'number')}{campo('dataVencimento', 'Data de vencimento', 'date')}<button disabled={busy === 'recebivel'}>Cadastrar recebível</button></form></section>

    <section id="liquidacao" hidden={secao !== 'todos' && secao !== 'liquidacao'}><h2>Liquidação</h2><form onSubmit={enviar('liquidar', async form => {
      const d = new FormData(form)
      const recebivelId = String(d.get('recebivelId'))
      const moedaPagamento = String(d.get('moedaPagamento')) as Moeda
      const operacaoAtual = operacaoLiquidacao.current

      if (
        !operacaoAtual ||
        operacaoAtual.recebivelId !== recebivelId ||
        operacaoAtual.moedaPagamento !== moedaPagamento
      ) {
        operacaoLiquidacao.current = {
          chave: crypto.randomUUID(),
          recebivelId,
          moedaPagamento,
        }
      }

      const liquidacao = await api.liquidar(
        recebivelId,
        moedaPagamento,
        operacaoLiquidacao.current.chave,
      )

      setResultadoLiquidacao(liquidacao)
      operacaoLiquidacao.current = null

      await carregar()
      setMensagem('Recebível liquidado.')
    })}><label>Recebível<select required name="recebivelId"><option value="">{recebiveisPendentes.length ? 'Selecione' : 'Nenhum recebível pendente'}</option>{recebiveisPendentes.map(recebivel => <option key={recebivel.id} value={recebivel.id}>{recebivel.tipo} · {dinheiro(recebivel.valorFace)} · {recebivel.dataVencimento}</option>)}</select></label><label>Moeda de pagamento<select name="moedaPagamento">{opcoesMoeda}</select></label><button className="liquidar-button" disabled={busy === 'liquidar' || !recebiveisPendentes.length}>Liquidar</button></form>{resultadoLiquidacao && <div className="resultado"><b>Resultado da liquidação</b><p>Valor presente <strong>{dinheiro(resultadoLiquidacao.valorPresente, 'BRL')}</strong></p><p>Deságio <strong>{dinheiro(resultadoLiquidacao.valorDesagio, 'BRL')}</strong></p><p>Prazo <strong>{resultadoLiquidacao.prazoMeses} meses</strong></p><p>Taxa base <strong>{percentual(resultadoLiquidacao.taxaBase)}</strong></p><p>Spread <strong>{percentual(resultadoLiquidacao.spread)}</strong></p>{resultadoLiquidacao.taxaCambioUtilizada != null && <p>Taxa de câmbio <strong>{resultadoLiquidacao.taxaCambioUtilizada}</strong></p>}<p className="total">Valor final <strong>{dinheiro(resultadoLiquidacao.valorFinal, resultadoLiquidacao.moedaPagamento)}</strong></p></div>}{!recebiveisPendentes.length && <p className="empty-hint">Cadastre um recebível ou selecione um que esteja pendente para liquidar.</p>}</section>

    <section id="extrato" hidden={secao !== 'todos' && secao !== 'extrato'}><h2>Extrato de liquidações</h2><form onSubmit={enviar('extrato', async form => {
      const d = new FormData(form); setLiquidacoes(await api.listarLiquidacoes({ dataInicio: String(d.get('dataInicio') || ''), dataFim: String(d.get('dataFim') || ''), cedenteId: String(d.get('cedenteId') || ''), moeda: String(d.get('moeda') || '') as Moeda }))
    })}>{campo('dataInicio', 'Data inicial', 'date', false)}{campo('dataFim', 'Data final', 'date', false)}<label>Cedente<select name="cedenteId"><option value="">Todos</option>{cedentes.map(cedente => <option key={cedente.id} value={cedente.id}>{cedente.nome}</option>)}</select></label><label>Moeda<select name="moeda"><option value="">Todas</option>{opcoesMoeda}</select></label><button disabled={busy === 'extrato'}>Filtrar</button></form>{liquidacoes.length > 0 && <table><thead><tr><th>Data</th><th>Recebível</th><th>Cedente</th><th>Moeda</th><th>Valor final</th></tr></thead><tbody>{liquidacoes.map(item => <tr key={item.id}><td>{new Date(item.liquidadoEm).toLocaleString('pt-BR')}</td><td><span className="uuid">{item.recebivelId}</span></td><td><span className="cedente-extrato" title={nomeCedente(item.recebivelId)}>{nomeCedente(item.recebivelId)}</span></td><td>{item.moedaPagamento}</td><td>{dinheiro(item.valorFinal, item.moedaPagamento)}</td></tr>)}</tbody></table>}</section>
  </main>
}

export default App
