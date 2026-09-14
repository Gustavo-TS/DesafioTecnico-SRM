export type Cedente={id:string;nome:string;documento:string;criadoEm:string}
export type Recebivel={id:string;cedenteId:string;tipo:string;valorFace:number;dataVencimento:string;status:string;criadoEm:string}
export type Resultado={valorFace:number;taxaBase:number;spread:number;prazoMeses:number;valorPresente:number;valorDesagio:number;moedaPagamento:string;taxaCambioUtilizada?:number|null;valorFinal:number}
export type Liquidacao=Resultado&{id:string;recebivelId:string;taxaCambioVigenteEm?:string|null;liquidadoEm:string}
const base=(import.meta.env.VITE_API_BASE_URL||'http://localhost:8080').replace(/\/$/,'')
async function errorMessage(response:Response){const text=await response.text();if(!text)return `Erro na requisição (${response.status}).`;try{const body=JSON.parse(text) as {message?:unknown};if(typeof body.message==='string'&&body.message)return body.message}catch{}return text}
async function call<T>(path:string,options:RequestInit={}):Promise<T>{const headers=new Headers(options.headers);if(options.body&&!headers.has('Content-Type'))headers.set('Content-Type','application/json');let response:Response;try{response=await fetch(base+path,{...options,headers})}catch{throw new Error('Não foi possível conectar à API. Verifique se o backend está em execução.')}if(!response.ok)throw new Error(await errorMessage(response));const text=await response.text();return text?JSON.parse(text) as T:undefined as T}
export const api={
 cedentes:()=>call<Cedente[]>('/api/cedentes'),recebiveis:()=>call<Recebivel[]>('/api/recebiveis'),
 cedente:(b:{nome:string;documento:string})=>call('/api/cedentes',{method:'POST',body:JSON.stringify(b)}),
 recebivel:(b:{cedenteId:string;tipo:string;valorFace:number;dataVencimento:string})=>call('/api/recebiveis',{method:'POST',body:JSON.stringify(b)}),
 cambio:(b:{moedaOrigem:string;moedaDestino:string;taxa:number;vigenteEm:string})=>call('/api/taxas-cambio',{method:'POST',body:JSON.stringify(b)}),
 simular:(b:Record<string,unknown>)=>call<Resultado>('/api/precificacoes/simular',{method:'POST',body:JSON.stringify(b)}),
 liquidar:(id:string,moedaPagamento:string)=>call<Liquidacao>(`/api/recebiveis/${id}/liquidacoes`,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify({moedaPagamento})}),
 liquidacoes:(f:Record<string,string>)=>{const {moedaPagamento,...filtros}=f;return call<Liquidacao[]>('/api/liquidacoes?'+new URLSearchParams(Object.entries({...filtros,moeda:moedaPagamento}).filter(([,v])=>v)))}
}
